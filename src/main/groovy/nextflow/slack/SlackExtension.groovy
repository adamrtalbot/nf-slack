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
import nextflow.Session
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint

/**
 * Implements custom Slack functions which can be called from
 * Nextflow scripts.
 *
 * Available functions:
 * - slackMessage(String): Send a simple text message
 * - slackMessage(Map): Send a rich formatted message
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
@Slf4j
@CompileStatic
class SlackExtension extends PluginExtensionPoint {

    @Override
    protected void init(Session session) {
        // No initialization needed
    }

    /**
     * Send a simple text message to Slack
     *
     * Example:
     * slackMessage("Analysis starting for sample ${sample_id}")
     *
     * @param text The message text to send
     */
    @Function
    void slackMessage(String text) {
        try {
            // Get the observer instance from factory
            def observer = SlackFactory.observerInstance

            if (!observer) {
                log.debug "Slack plugin: Observer not initialized, skipping message"
                return
            }

            if (!observer.client || !observer.messageBuilder) {
                log.debug "Slack plugin: Not configured, skipping message"
                return
            }

            // Build and send simple message
            def message = observer.messageBuilder.buildSimpleMessage(text)
            observer.client.sendMessage(message)

            log.debug "Slack plugin: Sent custom text message"

        } catch (Exception e) {
            log.error "Slack plugin: Error sending message: ${e.message}", e
            // Don't propagate exception - never fail the workflow
        }
    }

    /**
     * Send a rich formatted message to Slack
     *
     * Example:
     * slackMessage([
     *     message: "Analysis complete",
     *     color: "#2EB887",
     *     fields: [
     *         [title: "Sample", value: sample_id, short: true],
     *         [title: "Status", value: "Success", short: true]
     *     ]
     * ])
     *
     * @param options Map with keys:
     *   - message (required): The main message text
     *   - color (optional): Color bar for attachment (#2EB887 for success, #A30301 for error, #3AA3E3 for info)
     *   - fields (optional): List of field maps with title, value, and short (boolean)
     */
    @Function
    void slackMessage(Map options) {
        try {
            // Validate required parameters
            if (!options.message) {
                log.error "Slack plugin: 'message' parameter is required for rich messages"
                return
            }

            // Get the observer instance from factory
            def observer = SlackFactory.observerInstance

            if (!observer) {
                log.debug "Slack plugin: Observer not initialized, skipping message"
                return
            }

            if (!observer.client || !observer.messageBuilder) {
                log.debug "Slack plugin: Not configured, skipping message"
                return
            }

            // Build and send rich message
            def message = observer.messageBuilder.buildRichMessage(options)
            observer.client.sendMessage(message)

            log.debug "Slack plugin: Sent custom rich message"

        } catch (Exception e) {
            log.error "Slack plugin: Error sending rich message: ${e.message}", e
            // Don't propagate exception - never fail the workflow
        }
    }
}
