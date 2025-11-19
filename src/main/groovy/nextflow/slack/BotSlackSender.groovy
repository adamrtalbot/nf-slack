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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Slack sender implementation using Slack Bot Token API.
 *
 * This sender uses the chat.postMessage API endpoint with bot token authentication
 * to send messages to a specific channel. It supports Slack's Block Kit format
 * for rich message formatting.
 *
 * Authentication: Bot tokens (xoxb-*) or user tokens (xoxp-*)
 * API Endpoint: https://slack.com/api/chat.postMessage
 * Format: Block Kit JSON payload
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
@Slf4j
@CompileStatic
class BotSlackSender implements SlackSender {

    private static final String SLACK_API_URL = 'https://slack.com/api/chat.postMessage'

    private final String botToken
    private final String channelId
    private final Set<String> loggedErrors = new HashSet<>()

    /**
     * Create a new bot sender
     *
     * @param botToken Slack bot token (xoxb-* or xoxp-*)
     * @param channelId Slack channel ID (e.g., C1234567890)
     */
    BotSlackSender(String botToken, String channelId) {
        this.botToken = botToken
        this.channelId = channelId
    }

    /**
     * Send a message to Slack using the bot API
     *
     * @param message JSON payload containing Block Kit formatted message with 'text' and optionally 'blocks'
     */
    @Override
    void sendMessage(String message) {
        try {
            // Parse the incoming message and add channel parameter
            def parsed = new groovy.json.JsonSlurper().parseText(message) as Map
            def payload = new LinkedHashMap(parsed)
            payload.channel = channelId

            // Build JSON payload with channel
            def payloadJson = new groovy.json.JsonBuilder(payload).toString()

            def url = new URL(SLACK_API_URL)
            def connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = 'POST'
            connection.doOutput = true
            connection.setRequestProperty('Content-Type', 'application/json; charset=UTF-8')
            connection.setRequestProperty('Authorization', "Bearer ${botToken}")

            // Write the message payload
            connection.outputStream.withWriter('UTF-8') { writer ->
                writer.write(payloadJson)
            }
            connection.outputStream.close()

            // Check response
            def responseCode = connection.responseCode
            if (responseCode == 200) {
                // Parse response to check for Slack API errors
                def response = connection.inputStream.text
                def json = new groovy.json.JsonSlurper().parseText(response) as Map

                if (json.ok) {
                    log.debug "Slack bot: Message sent successfully to channel ${channelId}"
                } else {
                    def error = json.error as String
                    logErrorOnce("Slack bot API returned error: ${error}")
                }
            } else {
                logErrorOnce("Slack bot API returned HTTP ${responseCode}")
            }

        } catch (Exception e) {
            logErrorOnce("Failed to send Slack bot message: ${e.message}")
        }
    }

    /**
     * Log an error message only once to avoid spam
     * Uses a set to track which error messages have been logged
     */
    private void logErrorOnce(String errorMessage) {
        if (!loggedErrors.contains(errorMessage)) {
            log.error errorMessage
            loggedErrors.add(errorMessage)
        }
    }
}
