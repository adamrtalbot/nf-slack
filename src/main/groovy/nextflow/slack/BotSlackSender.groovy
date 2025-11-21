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

package nextflow.slack

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Slack sender implementation using Bot User OAuth Token.
 *
 * Features:
 * - Sends messages via chat.postMessage API
 * - Handles rate limiting
 * - Supports Block Kit (future)
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
@Slf4j
@CompileStatic
class BotSlackSender implements SlackSender {

    private static final String API_URL = "https://slack.com/api/chat.postMessage"

    private final String botToken
    private final String channelId
    private final Set<String> loggedErrors = Collections.synchronizedSet(new HashSet<String>())

    /**
     * Create a new BotSlackSender
     *
     * @param botToken Bot User OAuth Token (xoxb-...)
     * @param channelId Channel ID to send messages to
     */
    BotSlackSender(String botToken, String channelId) {
        this.botToken = botToken
        this.channelId = channelId
    }

    /**
     * Send a message to Slack via Web API
     *
     * @param message JSON message payload (must be compatible with chat.postMessage)
     */
    @Override
    void sendMessage(String message) {
        try {
            // Message is already formatted by SlackMessageBuilder with channel ID
            postToSlack(message)

        } catch (Exception e) {
            def errorMsg = "Slack plugin: Error sending bot message: ${e.message}".toString()
            if (loggedErrors.add(errorMsg)) {
                log.error errorMsg
            }
        }
    }

    private void postToSlack(String jsonPayload) {
        HttpURLConnection connection = null
        try {
            def url = new URL(API_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = 'POST'
            connection.doOutput = true
            connection.setRequestProperty('Content-Type', 'application/json; charset=utf-8')
            connection.setRequestProperty('Authorization', "Bearer ${botToken}")

            // Send message
            connection.outputStream.withCloseable { out ->
                out.write(jsonPayload.getBytes("UTF-8"))
            }

            // Check HTTP response
            def responseCode = connection.responseCode
            if (responseCode != 200) {
                def errorBody = connection.errorStream?.text ?: ""
                log.error "Slack plugin: HTTP ${responseCode}: ${errorBody}"
                return
            }

            // Check Slack API 'ok' status
            def responseText = connection.inputStream.text
            def response = new JsonSlurper().parseText(responseText) as Map

            if (!response.ok) {
                def error = response.error
                def errorMsg = "Slack plugin: API error: ${error}".toString()
                if (loggedErrors.add(errorMsg)) {
                    log.error errorMsg
                }
            }

        } catch (java.net.UnknownHostException e) {
            logError("Slack plugin: Unknown host (network issue): ${e.message}")
        } catch (java.net.SocketTimeoutException e) {
            logError("Slack plugin: Connection timeout: ${e.message}")
        } catch (IOException e) {
            logError("Slack plugin: Network error sending bot message: ${e.message}")
        } catch (groovy.json.JsonException e) {
            logError("Slack plugin: Error parsing Slack response: ${e.message}")
        } catch (Exception e) {
            logError("Slack plugin: Unexpected error sending bot message: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    private void logError(String message) {
        if (loggedErrors.add(message)) {
            log.error message
        }
    }
}
