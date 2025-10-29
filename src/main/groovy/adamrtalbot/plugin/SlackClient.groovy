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

/**
 * HTTP client for sending messages to Slack webhooks.
 *
 * Features:
 * - Synchronous message sending
 * - Graceful error handling (never fails workflow)
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
@Slf4j
@CompileStatic
class SlackClient {

    private final String webhookUrl

    /**
     * Create a new SlackClient with the given webhook URL
     */
    SlackClient(String webhookUrl) {
        this.webhookUrl = webhookUrl
    }

    /**
     * Send a message to Slack webhook
     *
     * @param message JSON message payload
     */
    void sendMessage(String message) {
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
                log.error "Slack webhook HTTP ${responseCode}: ${errorBody}"
            }

            connection.disconnect()
        } catch (Exception e) {
            log.debug "Slack plugin: Error sending message: ${e.message}"
        }
    }
}
