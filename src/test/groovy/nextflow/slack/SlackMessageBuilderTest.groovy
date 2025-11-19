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
import nextflow.Session
import nextflow.script.WorkflowMetadata
import nextflow.util.Duration
import spock.lang.Specification

/**
 * Tests for SlackMessageBuilder
 */
class SlackMessageBuilderTest extends Specification {

    def config
    def session
    def messageBuilder

    def setup() {
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            onStart: [
                enabled: true,
                includeCommandLine: true
            ],
            onComplete: [
                enabled: true,
                includeCommandLine: true,
                includeResourceUsage: true
            ],
            onError: [
                enabled: true,
                includeCommandLine: true
            ]
        ])

        session = Mock(Session)
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test-workflow.nf'
        session.workflowMetadata >> metadata
        session.runName >> 'test-run'
        session.uniqueId >> UUID.fromString('00000000-0000-0000-0000-000000000000')
        session.commandLine >> 'nextflow run test.nf'
        session.workDir >> java.nio.file.Paths.get('/work/dir')

        messageBuilder = new SlackMessageBuilder(config, session)
    }

    def 'should build workflow start message'() {
        when:
        def message = messageBuilder.buildWorkflowStartMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        json.text == 'Pipeline test-workflow.nf started'
        json.blocks.size() > 0

        // Check header (should not have emoji)
        def header = json.blocks.find { it.type == 'header' }
        header.text.text.contains('test-workflow.nf')
        !header.text.text.contains('🔵') // No emoji in header

        // Check main message section
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Pipeline started') }
        messageSection != null

        // Check for run name field
        def fieldSection = json.blocks.find { it.type == 'section' && it.fields }
        fieldSection.fields.any { it.text.contains('Run Name') && it.text.contains('test-run') }

        // Check for command line section
        def commandSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Command Line') }
        commandSection.text.text.contains('nextflow run test.nf')
    }

    def 'should build workflow complete message'() {
        given:
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test-workflow.nf'
        metadata.duration >> Duration.of('1h 30m')
        // Don't mock stats to avoid complexity - test basic message structure
        metadata.stats >> null
        session.workflowMetadata >> metadata

        when:
        def message = messageBuilder.buildWorkflowCompleteMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        json.text == 'Pipeline test-workflow.nf completed successfully'
        json.blocks.size() > 0

        // Check header (should not have emoji)
        def header = json.blocks.find { it.type == 'header' }
        header.text.text.contains('test-workflow.nf')
        !header.text.text.contains('✅') // No emoji in header

        // Check main message section
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Pipeline completed successfully') }
        messageSection != null

        // Check fields section
        def fieldSection = json.blocks.find { it.type == 'section' && it.fields }
        fieldSection.fields.any { it.text.contains('Status') && it.text.contains('Success') }
        fieldSection.fields.any { it.text.contains('Run Name') && it.text.contains('test-run') }
        fieldSection.fields.any { it.text.contains('Duration') }
    }

    def 'should build workflow error message'() {
        given:
        // Create a new session for this test with error metadata
        def errorSession = Mock(Session)
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test-workflow.nf'
        metadata.duration >> Duration.of('30m')
        metadata.errorMessage >> 'Process failed with exit code 1'
        errorSession.workflowMetadata >> metadata
        errorSession.runName >> 'test-run'
        errorSession.commandLine >> 'nextflow run test.nf'

        def builder = new SlackMessageBuilder(config, errorSession)

        def errorRecord = Mock(nextflow.trace.TraceRecord)
        errorRecord.get('process') >> 'FAILED_PROCESS'

        when:
        def message = builder.buildWorkflowErrorMessage(errorRecord)
        def json = new JsonSlurper().parseText(message)

        then:
        json.text == 'Pipeline test-workflow.nf failed'
        json.blocks.size() > 0

        // Check header (should not have emoji)
        def header = json.blocks.find { it.type == 'header' }
        header.text.text.contains('test-workflow.nf')
        !header.text.text.contains('❌') // No emoji in header

        // Check main message section
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Pipeline failed') }
        messageSection != null

        // Check fields sections (now split into two sections)
        def fieldSections = json.blocks.findAll { it.type == 'section' && it.fields }
        fieldSections.size() >= 2

        // Check for status and failed process in any field section
        def allFields = fieldSections.collectMany { it.fields }
        allFields.any { it.text.contains('Status') && it.text.contains('Failed') }
        allFields.any { it.text.contains('Failed Process') && it.text.contains('FAILED_PROCESS') }

        // Check error message section
        def errorSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Error Message') }
        errorSection.text.text.contains('Process failed')
    }

    def 'should build simple text message'() {
        when:
        def message = messageBuilder.buildSimpleMessage('Hello from workflow!')
        def json = new JsonSlurper().parseText(message)

        then:
        json.text == 'Hello from workflow!'
    }

    def 'should build rich message with custom fields'() {
        given:
        def options = [
            message: 'Analysis complete',
            color: '#2EB887',
            fields: [
                [title: 'Sample', value: 'sample123', short: true],
                [title: 'Status', value: 'Success', short: true]
            ]
        ]

        when:
        def message = messageBuilder.buildRichMessage(options)
        def json = new JsonSlurper().parseText(message)

        then:
        json.text == 'Analysis complete'
        json.blocks.size() > 0

        // Check main message section (no emoji)
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Analysis complete') }
        !messageSection.text.text.contains('✅')  // No emoji

        // Check fields section
        def fieldSection = json.blocks.find { it.type == 'section' && it.fields }
        fieldSection.fields.size() == 2
        fieldSection.fields.any { it.text.contains('Sample') && it.text.contains('sample123') }
        fieldSection.fields.any { it.text.contains('Status') && it.text.contains('Success') }
    }

    def 'should throw exception for rich message without message text'() {
        given:
        def options = [
            color: '#2EB887'
        ]

        when:
        messageBuilder.buildRichMessage(options)

        then:
        thrown(IllegalArgumentException)
    }

    def 'should truncate long error messages'() {
        given:
        def errorSession = Mock(Session)
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test-workflow.nf'
        metadata.duration >> Duration.of('1m')
        metadata.errorMessage >> ('x' * 600) // Long error message
        errorSession.workflowMetadata >> metadata
        errorSession.runName >> 'test-run'

        def builder = new SlackMessageBuilder(config, errorSession)

        when:
        def message = builder.buildWorkflowErrorMessage(null)
        def json = new JsonSlurper().parseText(message)

        then:
        def errorSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Error Message') }
        errorSection.text.text.length() < 700  // Truncated message + markup
        errorSection.text.text.contains('...')
    }

    def 'should not include command line when disabled'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            onStart: [
                enabled: true,
                includeCommandLine: false
            ]
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowStartMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        // Should not find a section containing "Command Line"
        !json.blocks.find { it.type == 'section' && it.text?.text?.contains('Command Line') }
    }

    def 'should use custom start message template'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            onStart: [
                enabled: true,
                message: '🎬 *Custom workflow is starting!*'
            ]
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowStartMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Custom workflow is starting') }
        messageSection.text.text == '🎬 *Custom workflow is starting!*'
    }

    def 'should use custom complete message template'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            onComplete: [
                enabled: true,
                message: '🎉 *Analysis finished successfully!*'
            ]
        ])
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test-workflow.nf'
        metadata.duration >> Duration.of('1h')
        session.workflowMetadata >> metadata
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowCompleteMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Analysis finished successfully') }
        messageSection.text.text == '🎉 *Analysis finished successfully!*'
    }

    def 'should use custom error message template'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            onError: [
                enabled: true,
                message: '💥 *Workflow encountered an error!*'
            ]
        ])
        def errorSession = Mock(Session)
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test-workflow.nf'
        metadata.duration >> Duration.of('30m')
        metadata.errorMessage >> 'Process failed'
        errorSession.workflowMetadata >> metadata
        errorSession.runName >> 'test-run'
        messageBuilder = new SlackMessageBuilder(config, errorSession)

        when:
        def message = messageBuilder.buildWorkflowErrorMessage(null)
        def json = new JsonSlurper().parseText(message)

        then:
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Workflow encountered an error') }
        messageSection.text.text == '💥 *Workflow encountered an error!*'
    }

    def 'should use default messages when custom templates not provided'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST'
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def startMessage = messageBuilder.buildWorkflowStartMessage()
        def startJson = new JsonSlurper().parseText(startMessage)

        then:
        def messageSection = startJson.blocks.find { it.type == 'section' && it.text?.text?.contains('Pipeline started') }
        messageSection.text.text == '🚀 *Pipeline started*'
    }

    def 'should use map-based custom start message with custom fields'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            onStart: [
                enabled: true,
                message: [
                    text: '🎬 *Custom pipeline starting*',
                    color: '#FF5733',
                    includeFields: ['runName', 'status'],
                    customFields: [
                        [title: 'Environment', value: 'Production', short: true]
                    ]
                ]
            ]
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowStartMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        // Check for Block Kit format
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text == '🎬 *Custom pipeline starting*' }
        messageSection != null

        // Check header has emoji (replaces color sidebar)
        def header = json.blocks.find { it.type == 'header' }
        header.text.text.contains('🔴') || header.text.text.contains('test-workflow.nf')

        // Check fields section
        def fieldsSection = json.blocks.find { it.type == 'section' && it.fields != null }
        fieldsSection.fields.find { it.text.contains('Run Name') && it.text.contains('test-run') }
        fieldsSection.fields.find { it.text.contains('Status') && it.text.contains('Running') }
        fieldsSection.fields.find { it.text.contains('Environment') && it.text.contains('Production') }
    }

    def 'should use map-based custom complete message with selective fields'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            onComplete: [
                enabled: true,
                message: [
                    text: '🎉 *Analysis finished!*',
                    color: '#00FF00',
                    includeFields: ['runName', 'duration', 'status'],
                    customFields: [
                        [title: 'Output Location', value: 's3://bucket/results', short: false]
                    ]
                ]
            ]
        ])
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test-workflow.nf'
        metadata.duration >> Duration.of('2h')
        session.workflowMetadata >> metadata
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowCompleteMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        // Check for Block Kit format
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text == '🎉 *Analysis finished!*' }
        messageSection != null

        // Check fields section
        def fieldsSection = json.blocks.find { it.type == 'section' && it.fields != null }
        fieldsSection.fields.find { it.text.contains('Run Name') }
        fieldsSection.fields.find { it.text.contains('Duration') }
        fieldsSection.fields.find { it.text.contains('Status') && it.text.contains('Success') }
        fieldsSection.fields.find { it.text.contains('Output Location') && it.text.contains('s3://bucket/results') }
    }

    def 'should use map-based custom error message with error fields'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            onError: [
                enabled: true,
                message: [
                    text: '💥 *Pipeline crashed!*',
                    color: '#FF0000',
                    includeFields: ['runName', 'duration', 'errorMessage', 'failedProcess'],
                    customFields: [
                        [title: 'Support', value: 'contact@example.com', short: true]
                    ]
                ]
            ]
        ])
        def errorSession = Mock(Session)
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test-workflow.nf'
        metadata.duration >> Duration.of('30m')
        metadata.errorMessage >> 'Out of memory error'
        errorSession.workflowMetadata >> metadata
        errorSession.runName >> 'test-run'
        messageBuilder = new SlackMessageBuilder(config, errorSession)

        def errorRecord = Mock(nextflow.trace.TraceRecord)
        errorRecord.get('process') >> 'FAILED_PROCESS'

        when:
        def message = messageBuilder.buildWorkflowErrorMessage(errorRecord)
        def json = new JsonSlurper().parseText(message)

        then:
        // Check for Block Kit format
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text == '💥 *Pipeline crashed!*' }
        messageSection != null

        // Check fields section
        def fieldsSection = json.blocks.find { it.type == 'section' && it.fields != null }
        fieldsSection.fields.find { it.text.contains('Run Name') }
        fieldsSection.fields.find { it.text.contains('Duration') }
        fieldsSection.fields.find { it.text.contains('Failed Process') && it.text.contains('`FAILED_PROCESS`') }
        fieldsSection.fields.find { it.text.contains('Support') && it.text.contains('contact@example.com') }

        // Error message appears in its own section
        def errorSection = json.blocks.find { it.type == 'section' && it.text?.text?.contains('Error Message') }
        errorSection.text.text.contains('Out of memory')
    }

    def 'should use default values when map config has minimal settings'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            onStart: [
                enabled: true,
                message: [
                    text: 'Starting...'
                ]
            ]
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowStartMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        // Check for Block Kit format
        def messageSection = json.blocks.find { it.type == 'section' && it.text?.text == 'Starting...' }
        messageSection != null

        // Check header (should not have emoji)
        def header = json.blocks.find { it.type == 'header' }
        !header.text.text.contains('🔵') // No emoji in header

        // No fields section should exist since includeFields not specified
        def fieldsSection = json.blocks.find { it.type == 'section' && it.fields != null }
        fieldsSection == null
    }
}
