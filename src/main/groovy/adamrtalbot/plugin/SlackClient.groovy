/*
 * Copyright 2025, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package adamrtalbot.plugin

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * HTTP client for sending messages to Slack webhooks.
 *
 * Features:
 * - Asynchronous message sending
 * - Automatic retry with exponential backoff
 * - Rate limiting protection
 * - Graceful error handling (never fails workflow)
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
@Slf4j
@CompileStatic
class SlackClient {

    private static final int MAX_RETRIES = 3
    private static final int INITIAL_RETRY_DELAY_MS = 1000
    private static final int MAX_RETRY_DELAY_MS = 10000
    private static final int CONNECT_TIMEOUT_MS = 5000
    private static final int READ_TIMEOUT_MS = 10000

    private final String webhookUrl
    private final ExecutorService executor
    private volatile long lastRequestTime = 0
    private static final long MIN_REQUEST_INTERVAL_MS = 1000 // Rate limit: max 1 request per second

    /**
     * Create a new SlackClient with the given webhook URL
     */
    SlackClient(String webhookUrl) {
        this.webhookUrl = webhookUrl
        this.executor = Executors.newSingleThreadExecutor() { runnable ->
            def thread = new Thread(runnable, 'slack-client')
            thread.daemon = true
            return thread
        }
    }

    /**
     * Send a message to Slack
     *
     * @param message JSON message payload
     */
    void sendMessage(String message) {
        doSendMessage(message)
    }

    /**
     * Send a message synchronously with retry logic
     *
     * @param message JSON message payload
     * @return true if message sent successfully, false otherwise
     */
    boolean sendMessageWithRetry(String message) {
        int attempt = 0
        int delay = INITIAL_RETRY_DELAY_MS

        while (attempt < MAX_RETRIES) {
            try {
                // Rate limiting: ensure minimum interval between requests
                def now = System.currentTimeMillis()
                def timeSinceLastRequest = now - lastRequestTime
                if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                    Thread.sleep(MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest)
                }

                // Send the message
                def success = doSendMessage(message)
                lastRequestTime = System.currentTimeMillis()

                if (success) {
                    if (attempt > 0) {
                        log.debug "Slack plugin: Message sent successfully after ${attempt} retries"
                    }
                    return true
                }

                attempt++
                if (attempt < MAX_RETRIES) {
                    log.warn "Slack plugin: Message send failed, retrying in ${delay}ms (attempt ${attempt}/${MAX_RETRIES})"
                    Thread.sleep(delay)
                    delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS) // Exponential backoff with cap
                }

            } catch (InterruptedException e) {
                log.warn "Slack plugin: Message sending interrupted"
                Thread.currentThread().interrupt()
                return false
            } catch (IllegalStateException e) {
                // Configuration error - don't retry
                log.debug "Slack plugin: Stopping retries due to configuration error"
                return false
            } catch (Exception e) {
                attempt++
                if (attempt < MAX_RETRIES) {
                    log.warn "Slack plugin: Error sending message (attempt ${attempt}/${MAX_RETRIES}): ${e.message}"
                    try {
                        Thread.sleep(delay)
                        delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS)
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt()
                        return false
                    }
                }
            }
        }

        log.error "Slack plugin: Failed to send message after ${MAX_RETRIES} attempts"
        return false
    }

    /**
     * Send a single message to Slack webhook
     *
     * @param message JSON message payload
     * @return true if successful, false otherwise
     */
    protected boolean doSendMessage(String message) {
        try {
            def url = new URL(webhookUrl)
            def connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = 'POST'
            connection.doOutput = true
            connection.setRequestProperty('Content-type', 'application/json')

            // Send message
            connection.outputStream.write(message.bytes)
            connection.outputStream.close()

            // Check response
            def responseCode = connection.responseCode
            if (responseCode != 200) {
                def errorBody = connection.errorStream?.text ?: ""
                log.warn "Slack webhook HTTP ${responseCode}: ${errorBody}"
                connection.disconnect()
                return false
            }

            connection.disconnect()
            return true
        } catch (Exception e) {
            log.debug "Slack plugin: Error sending message: ${e.message}"
            return false
        }
    }

    /**
     * Shutdown the executor and wait for pending messages
     */
    void shutdown() {
        try {
            log.debug "Slack plugin: Shutting down message sender"
            executor.shutdown()
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn "Slack plugin: Timeout waiting for pending messages, forcing shutdown"
                executor.shutdownNow()
            }
        } catch (InterruptedException e) {
            log.warn "Slack plugin: Interrupted during shutdown"
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}
