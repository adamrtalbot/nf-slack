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
import nextflow.Session
import nextflow.processor.TaskHandler
import nextflow.trace.TraceObserver
import nextflow.trace.TraceRecord

/**
 * Implements a trace observer that sends Slack notifications
 * for workflow lifecycle events.
 *
 * Features:
 * - Automatic notifications for workflow start, complete, and error
 * - Configurable via nextflow.config
 * - Graceful error handling that never fails the workflow
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
@Slf4j
@CompileStatic
class SlackObserver implements TraceObserver {

    private Session session
    private SlackConfig config
    private SlackClient client
    private SlackMessageBuilder messageBuilder
    private TraceRecord errorRecord

    /**
     * Called when the workflow is created
     */
    @Override
    void onFlowCreate(Session session) {
        this.session = session

        // Parse configuration - throws IllegalArgumentException if invalid
        this.config = SlackConfig.from(session)

        // If not configured or disabled, skip initialization
        if (!config?.isConfigured()) {
            log.debug "Slack plugin: Not configured or disabled, notifications will not be sent"
            return
        }

        // Initialize client and message builder
        this.client = new SlackClient(config.webhook)
        this.messageBuilder = new SlackMessageBuilder(config, session)

        log.debug "Slack plugin: Initialized successfully"

        // Send workflow started notification if enabled
        if (config.notifyOnStart) {
            def message = messageBuilder.buildWorkflowStartMessage()
            client.sendMessage(message)
            log.debug "Slack plugin: Sent workflow start notification"
        }
    }

    /**
     * Called when the workflow begins execution
     */
    @Override
    void onFlowBegin() {
        // Nothing to do here for now
    }

    /**
     * Called when the workflow completes successfully
     */
    @Override
    void onFlowComplete() {
        if (!isConfigured()) return

        if (config.notifyOnComplete) {
            def message = messageBuilder.buildWorkflowCompleteMessage()
            client.sendMessage(message)
            log.debug "Slack plugin: Sent workflow complete notification"
        }

        shutdownClient()
    }

    /**
     * Called when the workflow fails
     */
    @Override
    void onFlowError(TaskHandler handler, TraceRecord trace) {
        if (trace) {
            this.errorRecord = trace
        }

        if (!isConfigured()) return

        if (config.notifyOnError) {
            def message = messageBuilder.buildWorkflowErrorMessage(trace)
            client.sendMessage(message)
            log.debug "Slack plugin: Sent workflow error notification"
        }

        shutdownClient()
    }

    /**
     * Check if the observer is properly configured
     */
    private boolean isConfigured() {
        return config != null && client != null && messageBuilder != null
    }

    /**
     * Shutdown the client and wait for pending messages
     */
    private void shutdownClient() {
        client?.shutdown()
    }

    /**
     * Get the Slack client for use by extension functions
     */
    SlackClient getClient() {
        return client
    }

    /**
     * Set the Slack client (package-private for testing)
     */
    void setClient(SlackClient client) {
        this.client = client
    }

    /**
     * Get the message builder for use by extension functions
     */
    SlackMessageBuilder getMessageBuilder() {
        return messageBuilder
    }

    /**
     * Get the configuration
     */
    SlackConfig getConfig() {
        return config
    }
}
