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

    private static final String COLOR_SUCCESS = '#2EB887'
    private static final String COLOR_ERROR = '#A30301'
    private static final String COLOR_INFO = '#3AA3E3'
    private static final String NEXTFLOW_ICON = 'https://www.nextflow.io/docs/latest/_static/favicon.ico'

    private final SlackConfig config
    private final Session session

    SlackMessageBuilder(SlackConfig config, Session session) {
        this.config = config
        this.session = session
    }

    /**
     * Build message for workflow started event using Block Kit format
     */
    String buildWorkflowStartMessage() {
        def workflowName = session.workflowMetadata?.scriptName ?: 'Unknown workflow'
        def runName = session.runName ?: 'Unknown run'
        def timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        // Check if using custom message configuration
        if (config.onStart.message instanceof Map) {
            return buildCustomMessage(config.onStart.message as Map, workflowName, timestamp, 'started')
        }

        def messageText = config.onStart.message instanceof String ? config.onStart.message : '🚀 *Pipeline started*'
        def blocks = []

        // Header with workflow name
        blocks << [
            type: 'header',
            text: [
                type: 'plain_text',
                text: "🔵 ${workflowName}",
                emoji: true
            ]
        ]

        // Main message
        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: messageText
            ]
        ]

        // Run name field
        blocks << [
            type: 'section',
            fields: [
                [type: 'mrkdwn', text: "*Run Name*\n${runName}"]
            ]
        ]

        // Command line if configured
        if (config.onStart.includeCommandLine && session.commandLine) {
            blocks << [
                type: 'section',
                text: [
                    type: 'mrkdwn',
                    text: "*Command Line*\n```${session.commandLine}```"
                ]
            ]
        }

        // Working directory
        if (session.workDir) {
            blocks << [
                type: 'section',
                text: [
                    type: 'mrkdwn',
                    text: "*Work Directory*\n`${session.workDir}`"
                ]
            ]
        }

        // Footer
        blocks << [
            type: 'context',
            elements: [
                [
                    type: 'mrkdwn',
                    text: "Started at ${formatTimestamp(timestamp)}"
                ]
            ]
        ]

        def message = [
            text: "Pipeline ${workflowName} started",  // Fallback text
            blocks: blocks
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build message for workflow completed successfully using Block Kit format
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

        def messageText = config.onComplete.message instanceof String ? config.onComplete.message : '✅ *Pipeline completed successfully*'
        def blocks = []

        // Header with workflow name
        blocks << [
            type: 'header',
            text: [
                type: 'plain_text',
                text: "✅ ${workflowName}",
                emoji: true
            ]
        ]

        // Main message
        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: messageText
            ]
        ]

        // Fields section (run name, duration, status)
        def fieldsList = []
        fieldsList << [type: 'mrkdwn', text: "*Run Name*\n${runName}"]
        fieldsList << [type: 'mrkdwn', text: "*Duration*\n${duration.toString()}"]
        fieldsList << [type: 'mrkdwn', text: "*Status*\n✅ Success"]

        // Add resource usage if configured
        if (config.onComplete.includeResourceUsage) {
            def stats = session.workflowMetadata?.stats
            if (stats) {
                def resourceInfo = []
                if (stats.cachedCount) resourceInfo << "Cached: ${stats.cachedCount}"
                if (stats.succeedCount) resourceInfo << "Completed: ${stats.succeedCount}"
                if (stats.failedCount) resourceInfo << "Failed: ${stats.failedCount}"

                if (resourceInfo) {
                    fieldsList << [type: 'mrkdwn', text: "*Tasks*\n${resourceInfo.join(', ')}"]
                }
            }
        }

        blocks << [
            type: 'section',
            fields: fieldsList
        ]

        // Footer
        blocks << [
            type: 'context',
            elements: [
                [
                    type: 'mrkdwn',
                    text: "Completed at ${formatTimestamp(timestamp)}"
                ]
            ]
        ]

        def message = [
            text: "Pipeline ${workflowName} completed successfully",  // Fallback text
            blocks: blocks
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build message for workflow error using Block Kit format
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

        def messageText = config.onError.message instanceof String ? config.onError.message : '❌ *Pipeline failed*'
        def blocks = []

        // Header with workflow name
        blocks << [
            type: 'header',
            text: [
                type: 'plain_text',
                text: "❌ ${workflowName}",
                emoji: true
            ]
        ]

        // Main message
        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: messageText
            ]
        ]

        // Fields section (run name, duration, status)
        def fieldsList = []
        fieldsList << [type: 'mrkdwn', text: "*Run Name*\n${runName}"]
        fieldsList << [type: 'mrkdwn', text: "*Duration*\n${duration.toString()}"]
        fieldsList << [type: 'mrkdwn', text: "*Status*\n❌ Failed"]

        // Add failed process info if available
        if (errorRecord) {
            def processName = errorRecord.get('process')
            if (processName) {
                fieldsList << [type: 'mrkdwn', text: "*Failed Process*\n`${processName}`"]
            }
        }

        blocks << [
            type: 'section',
            fields: fieldsList
        ]

        // Error message
        def truncatedError = errorMessage.take(500)
        if (errorMessage.length() > 500) {
            truncatedError += '...'
        }
        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: "*Error Message*\n```${truncatedError}```"
            ]
        ]

        // Command line if configured
        if (config.onError.includeCommandLine && session.commandLine) {
            blocks << [
                type: 'section',
                text: [
                    type: 'mrkdwn',
                    text: "*Command Line*\n```${session.commandLine}```"
                ]
            ]
        }

        // Footer
        blocks << [
            type: 'context',
            elements: [
                [
                    type: 'mrkdwn',
                    text: "Failed at ${formatTimestamp(timestamp)}"
                ]
            ]
        ]

        def message = [
            text: "Pipeline ${workflowName} failed",  // Fallback text
            blocks: blocks
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build a simple text message
     */
    String buildSimpleMessage(String text) {
        def message = [
            text: text
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build a rich message with custom formatting using Block Kit
     *
     * @param options Map with keys: message (required), color, fields (list of maps with title/value/short)
     */
    String buildRichMessage(Map options) {
        if (!options.message) {
            throw new IllegalArgumentException("Message text is required")
        }

        def messageText = options.message as String
        def fields = options.fields as List ?: []
        def color = options.color as String ?: COLOR_INFO
        def blocks = []

        // Main message section
        def emoji = getColorEmoji(color)
        def headerText = emoji ? "${emoji} ${messageText}" : messageText

        blocks << [
            type: 'section',
            text: [
                type: 'mrkdwn',
                text: headerText
            ]
        ]

        // Add fields if provided
        if (fields) {
            def fieldsList = []
            for (def fieldObj : fields) {
                def field = fieldObj as Map
                def title = field.title as String
                def value = field.value as String
                fieldsList << [type: 'mrkdwn', text: "*${title}*\n${value}"]
            }

            blocks << [
                type: 'section',
                fields: fieldsList
            ]
        }

        def message = [
            text: messageText,  // Fallback text
            blocks: blocks
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build a custom message using map configuration
     *
     * @param customConfig Map with keys: text, color, includeFields, customFields
     * @param workflowName Name of the workflow
     * @param timestamp ISO timestamp
     * @param status Workflow status (started, completed, failed)
     * @param errorRecord Optional error record for failed workflows
     */
    private String buildCustomMessage(Map customConfig, String workflowName, String timestamp, String status, TraceRecord errorRecord = null) {
        def runName = session.runName ?: 'Unknown run'
        def duration = session.workflowMetadata?.duration ?: Duration.of(0)
        def errorMessage = session.workflowMetadata?.errorMessage ?: 'Unknown error'

        // Get message text
        def messageText = customConfig.text ?: getDefaultMessageText(status)

        // Get color
        def color = customConfig.color ?: getDefaultColor(status)

        // Build fields array
        def fields = []

        // Add default fields if specified
        def includeFields = customConfig.includeFields as List ?: []
        if (includeFields) {
            if (includeFields.contains('runName')) {
                fields << [title: 'Run Name', value: runName, short: true]
            }
            if (includeFields.contains('duration') && status != 'started') {
                fields << [title: 'Duration', value: duration.toString(), short: true]
            }
            if (includeFields.contains('status')) {
                fields << [title: 'Status', value: getStatusEmoji(status), short: true]
            }
            if (includeFields.contains('commandLine') && session.commandLine) {
                fields << [title: 'Command Line', value: "```${session.commandLine}```", short: false]
            }
            if (includeFields.contains('workDir') && session.workDir && status == 'started') {
                fields << [title: 'Work Directory', value: "`${session.workDir}`", short: false]
            }
            if (includeFields.contains('errorMessage') && status == 'failed') {
                fields << [title: 'Error Message', value: "```${errorMessage.take(500)}${errorMessage.length() > 500 ? '...' : ''}```", short: false]
            }
            if (includeFields.contains('failedProcess') && errorRecord) {
                def processName = errorRecord.get('process')
                if (processName) {
                    fields << [title: 'Failed Process', value: "`${processName}`", short: true]
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
                        fields << [title: 'Tasks', value: resourceInfo.join(', '), short: true]
                    }
                }
            }
        }

        // Add custom fields
        def customFields = customConfig.customFields as List ?: []
        fields.addAll(customFields)

        // Build footer text
        def footerText = "Workflow ${status} at ${formatTimestamp(timestamp)}"

        def message = [
            attachments: [
                [
                    fallback: "Pipeline ${workflowName} ${status}",
                    color: color,
                    author_name: workflowName,
                    author_icon: NEXTFLOW_ICON,
                    text: messageText,
                    fields: fields,
                    footer: footerText,
                    ts: System.currentTimeMillis() / 1000 as long
                ]
            ]
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
     * Get default color for a status
     */
    private static String getDefaultColor(String status) {
        switch (status) {
            case 'started':
                return COLOR_INFO
            case 'completed':
                return COLOR_SUCCESS
            case 'failed':
                return COLOR_ERROR
            default:
                return COLOR_INFO
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

    /**
     * Get emoji representing a color
     */
    private static String getColorEmoji(String color) {
        switch (color) {
            case COLOR_SUCCESS:
                return '✅'
            case COLOR_ERROR:
                return '❌'
            case COLOR_INFO:
                return '🔵'
            default:
                return ''
        }
    }
}
