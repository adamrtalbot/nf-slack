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
     * Build message for workflow started event
     */
    String buildWorkflowStartMessage() {
        def workflowName = session.workflowMetadata?.scriptName ?: 'Unknown workflow'
        def runName = session.runName ?: 'Unknown run'
        def timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        def fields = []

        // Add run name
        fields << [
            title: 'Run Name',
            value: runName,
            short: true
        ]

        // Session ID removed for MVP - was causing test issues with UUID mocking
        // Can be added back in future version if needed

        // Add command line if configured
        if (config.includeCommandLine && session.commandLine) {
            fields << [
                title: 'Command Line',
                value: "```${session.commandLine}```",
                short: false
            ]
        }

        // Add working directory
        if (session.workDir) {
            fields << [
                title: 'Work Directory',
                value: "`${session.workDir}`",
                short: false
            ]
        }

        def message = [
            username: config.username,
            icon_emoji: config.iconEmoji,
            attachments: [
                [
                    fallback: "Pipeline ${workflowName} started",
                    color: COLOR_INFO,
                    author_name: workflowName,
                    author_icon: NEXTFLOW_ICON,
                    text: "🚀 *Pipeline started*",
                    fields: fields,
                    footer: "Started at ${formatTimestamp(timestamp)}",
                    ts: System.currentTimeMillis() / 1000 as long
                ]
            ]
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

        def fields = []

        // Add run name
        fields << [
            title: 'Run Name',
            value: runName,
            short: true
        ]

        // Add duration
        fields << [
            title: 'Duration',
            value: duration.toString(),
            short: true
        ]

        // Add success status
        fields << [
            title: 'Status',
            value: '✅ Success',
            short: true
        ]

        // Add resource usage if configured
        if (config.includeResourceUsage) {
            def stats = session.workflowMetadata?.stats
            if (stats) {
                def resourceInfo = []
                if (stats.cachedCount) resourceInfo << "Cached: ${stats.cachedCount}"
                if (stats.succeedCount) resourceInfo << "Completed: ${stats.succeedCount}"
                if (stats.failedCount) resourceInfo << "Failed: ${stats.failedCount}"

                if (resourceInfo) {
                    fields << [
                        title: 'Tasks',
                        value: resourceInfo.join(', '),
                        short: true
                    ]
                }
            }
        }

        def message = [
            username: config.username,
            icon_emoji: config.iconEmoji,
            attachments: [
                [
                    fallback: "Pipeline ${workflowName} completed successfully",
                    color: COLOR_SUCCESS,
                    author_name: workflowName,
                    author_icon: NEXTFLOW_ICON,
                    text: "✅ *Pipeline completed successfully*",
                    fields: fields,
                    footer: "Completed at ${formatTimestamp(timestamp)}",
                    ts: System.currentTimeMillis() / 1000 as long
                ]
            ]
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

        def fields = []

        // Add run name
        fields << [
            title: 'Run Name',
            value: runName,
            short: true
        ]

        // Add duration
        fields << [
            title: 'Duration',
            value: duration.toString(),
            short: true
        ]

        // Add status
        fields << [
            title: 'Status',
            value: '❌ Failed',
            short: true
        ]

        // Add error message
        fields << [
            title: 'Error Message',
            value: "```${errorMessage.take(500)}${errorMessage.length() > 500 ? '...' : ''}```",
            short: false
        ]

        // Add failed process info if available
        if (errorRecord) {
            def processName = errorRecord.get('process')
            if (processName) {
                fields << [
                    title: 'Failed Process',
                    value: "`${processName}`",
                    short: true
                ]
            }
        }

        // Add command line if configured
        if (config.includeCommandLine && session.commandLine) {
            fields << [
                title: 'Command Line',
                value: "```${session.commandLine}```",
                short: false
            ]
        }

        def message = [
            username: config.username,
            icon_emoji: config.iconEmoji,
            attachments: [
                [
                    fallback: "Pipeline ${workflowName} failed",
                    color: COLOR_ERROR,
                    author_name: workflowName,
                    author_icon: NEXTFLOW_ICON,
                    text: "❌ *Pipeline failed*",
                    fields: fields,
                    footer: "Failed at ${formatTimestamp(timestamp)}",
                    ts: System.currentTimeMillis() / 1000 as long
                ]
            ]
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build a simple text message
     */
    String buildSimpleMessage(String text) {
        def message = [
            username: config.username,
            icon_emoji: config.iconEmoji,
            text: text
        ]

        return new JsonBuilder(message).toPrettyString()
    }

    /**
     * Build a rich message with custom formatting
     *
     * @param options Map with keys: message (required), color, fields (list of maps with title/value/short)
     */
    String buildRichMessage(Map options) {
        if (!options.message) {
            throw new IllegalArgumentException("Message text is required")
        }

        def fields = options.fields as List ?: []
        def color = options.color as String ?: COLOR_INFO

        def message = [
            username: config.username,
            icon_emoji: config.iconEmoji,
            attachments: [
                [
                    fallback: options.message as String,
                    color: color,
                    text: options.message as String,
                    fields: fields,
                    ts: System.currentTimeMillis() / 1000 as long
                ]
            ]
        ]

        return new JsonBuilder(message).toPrettyString()
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
