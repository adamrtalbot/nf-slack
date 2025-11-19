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

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.trace.TraceRecord
import nextflow.util.Duration

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Builds Slack messages using Block Kit format for workflow events.
 *
 * Supports creating formatted messages for:
 * - Workflow started
 * - Workflow completed successfully
 * - Workflow failed
 * - Custom messages
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
@Slf4j
@CompileStatic
class SlackMessageBuilder {

    private static final String NEXTFLOW_ICON = 'https://www.nextflow.io/docs/latest/_static/favicon.ico'

    private final SlackConfig config
    private final Session session

    SlackMessageBuilder(SlackConfig config, Session session) {
        this.config = config
        this.session = session
    }

    /**
     * Build message for workflow started event
     */
    String buildWorkflowStartMessage() {
        def workflowName = session.workflowMetadata?.scriptName ?: 'Unknown workflow'
        def runName = session.runName ?: 'Unknown run'
        def timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        // Check if using custom message configuration
        if (config.onStart.message instanceof Map) {
            return buildCustomMessage(config.onStart.message as Map, workflowName, timestamp, 'started')
        }

        def blocks = []

        // Header section
        def messageText = config.onStart.message instanceof String ? config.onStart.message : '🚀 *Pipeline started*'
        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: messageText
            ]
        ]

        def fields = []

        // Add run name
        fields << [
            type: 'mrkdwn',
            text: "*Run Name*\n${runName}"
        ]

        // Add command line if configured
        if (config.onStart.includeCommandLine && session.commandLine) {
            // Command line is long, maybe put it in a separate section or just append to fields if it fits?
            // Fields in blocks are limited to 10 items.
            // Let's put command line in a separate section to avoid cluttering the grid if it's long.
            // But for consistency with previous design, let's try to fit it or use a separate block.
            // The previous design had `short: false` for command line.
            // In Block Kit, fields are always side-by-side (2 columns).
            // If we want a full width line, we should use a separate section or context.
            // Let's keep it in fields for now if it fits, or use a separate section.
            // Actually, `short: false` in attachments meant it took the full width.
            // In Block Kit fields, you can't control width per item easily.
            // So for long items like command line, it's better to have a separate section.
        }

        // Add working directory
        if (session.workDir) {
             fields << [
                type: 'mrkdwn',
                text: "*Work Directory*\n`${session.workDir}`"
            ]
        }

        if (fields) {
            blocks << [
                type: 'section',
                fields: fields
            ]
        }

        // Add command line in a separate section if configured
        if (config.onStart.includeCommandLine && session.commandLine) {
            blocks << [
                type: 'section',
                text: [
                    type: 'mrkdwn',
                    text: "*Command Line*\n```${session.commandLine}```"
                ]
            ]
        }

        // Context / Footer
        blocks << [
            type: 'context',
            elements: [
                [
                    type: 'mrkdwn',
                    text: "Started at ${formatTimestamp(timestamp)} | ${workflowName}"
                ]
            ]
        ]

        def message = [
            blocks: blocks
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build message for workflow completed successfully
     */
    String buildWorkflowCompleteMessage() {
        def workflowName = session.workflowMetadata?.scriptName ?: 'Unknown workflow'
        def runName = session.runName ?: 'Unknown run'
        def duration = session.workflowMetadata?.duration ?: Duration.of(0)
        def timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        // Check if using custom message configuration
        if (config.onComplete.message instanceof Map) {
            return buildCustomMessage(config.onComplete.message as Map, workflowName, timestamp, 'completed')
        }

        def blocks = []

        def messageText = config.onComplete.message instanceof String ? config.onComplete.message : '✅ *Pipeline completed successfully*'
        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: messageText
            ]
        ]

        def fields = []

        // Add run name
        fields << [
            type: 'mrkdwn',
            text: "*Run Name*\n${runName}"
        ]

        // Add duration
        fields << [
            type: 'mrkdwn',
            text: "*Duration*\n${duration.toString()}"
        ]

        // Add success status
        fields << [
            type: 'mrkdwn',
            text: "*Status*\n✅ Success"
        ]

        // Add resource usage if configured
        if (config.onComplete.includeResourceUsage) {
            def stats = session.workflowMetadata?.stats
            if (stats) {
                def resourceInfo = []
                if (stats.cachedCount) resourceInfo << "Cached: ${stats.cachedCount}"
                if (stats.succeedCount) resourceInfo << "Completed: ${stats.succeedCount}"
                if (stats.failedCount) resourceInfo << "Failed: ${stats.failedCount}"

                if (resourceInfo) {
                    fields << [
                        type: 'mrkdwn',
                        text: "*Tasks*\n${resourceInfo.join(', ')}"
                    ]
                }
            }
        }

        if (fields) {
            blocks << [
                type: 'section',
                fields: fields
            ]
        }

        // Context / Footer
        blocks << [
            type: 'context',
            elements: [
                [
                    type: 'mrkdwn',
                    text: "Completed at ${formatTimestamp(timestamp)} | ${workflowName}"
                ]
            ]
        ]

        def message = [
            blocks: blocks
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build message for workflow error
     */
    String buildWorkflowErrorMessage(TraceRecord errorRecord) {
        def workflowName = session.workflowMetadata?.scriptName ?: 'Unknown workflow'
        def runName = session.runName ?: 'Unknown run'
        def duration = session.workflowMetadata?.duration ?: Duration.of(0)
        def timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        def errorMessage = session.workflowMetadata?.errorMessage ?: 'Unknown error'

        // Check if using custom message configuration
        if (config.onError.message instanceof Map) {
            return buildCustomMessage(config.onError.message as Map, workflowName, timestamp, 'failed', errorRecord)
        }

        def blocks = []

        def messageText = config.onError.message instanceof String ? config.onError.message : '❌ *Pipeline failed*'
        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: messageText
            ]
        ]

        def fields = []

        // Add run name
        fields << [
            type: 'mrkdwn',
            text: "*Run Name*\n${runName}"
        ]

        // Add duration
        fields << [
            type: 'mrkdwn',
            text: "*Duration*\n${duration.toString()}"
        ]

        // Add status
        fields << [
            type: 'mrkdwn',
            text: "*Status*\n❌ Failed"
        ]

        // Add failed process info if available
        if (errorRecord) {
            def processName = errorRecord.get('process')
            if (processName) {
                fields << [
                    type: 'mrkdwn',
                    text: "*Failed Process*\n`${processName}`"
                ]
            }
        }

        if (fields) {
            blocks << [
                type: 'section',
                fields: fields
            ]
        }

        // Add error message in a separate section (it can be long)
        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: "*Error Message*\n```${errorMessage.take(2000)}${errorMessage.length() > 2000 ? '...' : ''}```"
            ]
        ]

        // Add command line if configured
        if (config.onError.includeCommandLine && session.commandLine) {
            blocks << [
                type: 'section',
                text: [
                    type: 'mrkdwn',
                    text: "*Command Line*\n```${session.commandLine}```"
                ]
            ]
        }

        // Context / Footer
        blocks << [
            type: 'context',
            elements: [
                [
                    type: 'mrkdwn',
                    text: "Failed at ${formatTimestamp(timestamp)} | ${workflowName}"
                ]
            ]
        ]

        def message = [
            blocks: blocks
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build a simple text message
     */
    String buildSimpleMessage(String text) {
        def message = [
            blocks: [
                [
                    type: 'section',
                    text: [
                        type: 'mrkdwn',
                        text: text
                    ]
                ]
            ]
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build a rich message with custom formatting
     *
     * @param options Map with keys: message (required), fields (list of maps with title/value/short)
     */
    String buildRichMessage(Map options) {
        if (!options.message) {
            throw new IllegalArgumentException("Message text is required")
        }

        def blocks = []

        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: options.message as String
            ]
        ]

        def fieldsList = options.fields as List ?: []
        if (fieldsList) {
            def fields = []
            fieldsList.each { field ->
                def f = field as Map
                fields << [
                    type: 'mrkdwn',
                    text: "*${f.title}*\n${f.value}"
                ]
            }
            blocks << [
                type: 'section',
                fields: fields
            ]
        }

        def message = [
            blocks: blocks
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build a custom message using map configuration
     */
    private String buildCustomMessage(Map customConfig, String workflowName, String timestamp, String status, TraceRecord errorRecord = null) {
        def runName = session.runName ?: 'Unknown run'
        def duration = session.workflowMetadata?.duration ?: Duration.of(0)
        def errorMessage = session.workflowMetadata?.errorMessage ?: 'Unknown error'

        def blocks = []

        // Get message text
        def messageText = customConfig.text ?: getDefaultMessageText(status)
        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: messageText
            ]
        ]

        // Build fields
        def fields = []

        // Add default fields if specified
        def includeFields = customConfig.includeFields as List ?: []
        if (includeFields) {
            if (includeFields.contains('runName')) {
                fields << [type: 'mrkdwn', text: "*Run Name*\n${runName}"]
            }
            if (includeFields.contains('duration') && status != 'started') {
                fields << [type: 'mrkdwn', text: "*Duration*\n${duration.toString()}"]
            }
            if (includeFields.contains('status')) {
                fields << [type: 'mrkdwn', text: "*Status*\n${getStatusEmoji(status)}"]
            }
            if (includeFields.contains('failedProcess') && errorRecord) {
                def processName = errorRecord.get('process')
                if (processName) {
                    fields << [type: 'mrkdwn', text: "*Failed Process*\n`${processName}`"]
                }
            }
            if (includeFields.contains('tasks') && status == 'completed') {
                def stats = session.workflowMetadata?.stats
                if (stats) {
                    def resourceInfo = []
                    if (stats.cachedCount) resourceInfo << "Cached: ${stats.cachedCount}"
                    if (stats.succeedCount) resourceInfo << "Completed: ${stats.succeedCount}"
                    if (stats.failedCount) resourceInfo << "Failed: ${stats.failedCount}"
                    if (resourceInfo) {
                        fields << [type: 'mrkdwn', text: "*Tasks*\n${resourceInfo.join(', ')}"]
                    }
                }
            }
        }

        // Add custom fields
        def customFields = customConfig.customFields as List ?: []
        customFields.each { field ->
            def f = field as Map
            fields << [
                type: 'mrkdwn',
                text: "*${f.title}*\n${f.value}"
            ]
        }

        if (fields) {
            blocks << [
                type: 'section',
                fields: fields
            ]
        }

        // Handle long fields that should be their own sections
        if (includeFields.contains('commandLine') && session.commandLine) {
            blocks << [
                type: 'section',
                text: [
                    type: 'mrkdwn',
                    text: "*Command Line*\n```${session.commandLine}```"
                ]
            ]
        }
        if (includeFields.contains('workDir') && session.workDir && status == 'started') {
            blocks << [
                type: 'section',
                text: [
                    type: 'mrkdwn',
                    text: "*Work Directory*\n`${session.workDir}`"
                ]
            ]
        }
        if (includeFields.contains('errorMessage') && status == 'failed') {
            blocks << [
                type: 'section',
                text: [
                    type: 'mrkdwn',
                    text: "*Error Message*\n```${errorMessage.take(2000)}${errorMessage.length() > 2000 ? '...' : ''}```"
                ]
            ]
        }

        // Footer
        blocks << [
            type: 'context',
            elements: [
                [
                    type: 'mrkdwn',
                    text: "Workflow ${status} at ${formatTimestamp(timestamp)} | ${workflowName}"
                ]
            ]
        ]

        def message = [
            blocks: blocks
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Get default message text for a status
     */
    private static String getDefaultMessageText(String status) {
        switch (status) {
            case 'started':
                return '🚀 *Pipeline started*'
            case 'completed':
                return '✅ *Pipeline completed successfully*'
            case 'failed':
                return '❌ *Pipeline failed*'
            default:
                return '*Pipeline event*'
        }
    }

    /**
     * Get status emoji
     */
    private static String getStatusEmoji(String status) {
        switch (status) {
            case 'started':
                return '🚀 Running'
            case 'completed':
                return '✅ Success'
            case 'failed':
                return '❌ Failed'
            default:
                return 'Unknown'
        }
    }

    /**
     * Format timestamp for display
     */
    private static String formatTimestamp(String isoTimestamp) {
        try {
            def dateTime = OffsetDateTime.parse(isoTimestamp)
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"))
        } catch (Exception e) {
            return isoTimestamp
        }
    }
}
