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
            webhook: [
                url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
            ],
            includeCommandLine: true,
            includeResourceUsage: true
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
        json.attachments.size() == 1
        json.attachments[0].author_name == 'test-workflow.nf'
        json.attachments[0].text.contains('Pipeline started')
        json.attachments[0].color == '#3AA3E3' // info color

        // Check fields
        def fields = json.attachments[0].fields
        fields.find { it.title == 'Run Name' }.value == 'test-run'
        // Session ID may or may not be present depending on mock behavior
        fields.find { it.title == 'Command Line' }.value == '```nextflow run test.nf```'
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
        json.attachments.size() == 1
        json.attachments[0].text.contains('Pipeline completed successfully')
        json.attachments[0].color == '#2EB887' // success color

        // Check basic fields
        def fields = json.attachments[0].fields
        fields.find { it.title == 'Status' }.value == '✅ Success'
        fields.find { it.title == 'Run Name' }.value == 'test-run'
        fields.find { it.title == 'Duration' }
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
        json.attachments.size() == 1
        json.attachments[0].text.contains('Pipeline failed')
        json.attachments[0].color == '#A30301' // error color

        // Check fields
        def fields = json.attachments[0].fields
        fields.find { it.title == 'Status' }.value == '❌ Failed'
        fields.find { it.title == 'Error Message' }.value.contains('Process failed')
        fields.find { it.title == 'Failed Process' }.value == '`FAILED_PROCESS`'
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
        json.attachments.size() == 1
        json.attachments[0].text == 'Analysis complete'
        json.attachments[0].color == '#2EB887'
        json.attachments[0].fields.size() == 2
        json.attachments[0].fields[0].title == 'Sample'
        json.attachments[0].fields[0].value == 'sample123'
        json.attachments[0].fields[0].short == true
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
        def errorField = json.attachments[0].fields.find { it.title == 'Error Message' }
        errorField.value.length() < 600
        errorField.value.contains('...')
    }

    def 'should not include command line when disabled'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: [
                url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
            ],
            includeCommandLine: false
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowStartMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        def fields = json.attachments[0].fields
        !fields.find { it.title == 'Command Line' }
    }

    def 'should use custom start message template'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: [
                url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
            ],
            startMessage: '🎬 *Custom workflow is starting!*'
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowStartMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        json.attachments[0].text == '🎬 *Custom workflow is starting!*'
    }

    def 'should use custom complete message template'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: [
                url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
            ],
            completeMessage: '🎉 *Analysis finished successfully!*'
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
        json.attachments[0].text == '🎉 *Analysis finished successfully!*'
    }

    def 'should use custom error message template'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: [
                url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
            ],
            errorMessage: '💥 *Workflow encountered an error!*'
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
        json.attachments[0].text == '💥 *Workflow encountered an error!*'
    }

    def 'should use default messages when custom templates not provided'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: [
                url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
            ]
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def startMessage = messageBuilder.buildWorkflowStartMessage()
        def startJson = new JsonSlurper().parseText(startMessage)

        then:
        startJson.attachments[0].text == '🚀 *Pipeline started*'
    }

    def 'should use map-based custom start message with custom fields'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            startMessage: [
                text: '🎬 *Custom pipeline starting*',
                color: '#FF5733',
                includeFields: ['runName', 'status'],
                customFields: [
                    [title: 'Environment', value: 'Production', short: true]
                ]
            ]
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowStartMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        json.attachments[0].text == '🎬 *Custom pipeline starting*'
        json.attachments[0].color == '#FF5733'
        json.attachments[0].fields.find { it.title == 'Run Name' }?.value == 'test-run'
        json.attachments[0].fields.find { it.title == 'Status' }?.value == '🚀 Running'
        json.attachments[0].fields.find { it.title == 'Environment' }?.value == 'Production'
    }

    def 'should use map-based custom complete message with selective fields'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            completeMessage: [
                text: '🎉 *Analysis finished!*',
                color: '#00FF00',
                includeFields: ['runName', 'duration', 'status'],
                customFields: [
                    [title: 'Output Location', value: 's3://bucket/results', short: false]
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
        json.attachments[0].text == '🎉 *Analysis finished!*'
        json.attachments[0].color == '#00FF00'
        json.attachments[0].fields.find { it.title == 'Run Name' }
        json.attachments[0].fields.find { it.title == 'Duration' }
        json.attachments[0].fields.find { it.title == 'Status' }?.value == '✅ Success'
        json.attachments[0].fields.find { it.title == 'Output Location' }?.value == 's3://bucket/results'
    }

    def 'should use map-based custom error message with error fields'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            errorMessage: [
                text: '💥 *Pipeline crashed!*',
                color: '#FF0000',
                includeFields: ['runName', 'duration', 'errorMessage', 'failedProcess'],
                customFields: [
                    [title: 'Support', value: 'contact@example.com', short: true]
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
        json.attachments[0].text == '💥 *Pipeline crashed!*'
        json.attachments[0].color == '#FF0000'
        json.attachments[0].fields.find { it.title == 'Run Name' }
        json.attachments[0].fields.find { it.title == 'Duration' }
        json.attachments[0].fields.find { it.title == 'Error Message' }?.value.contains('Out of memory')
        json.attachments[0].fields.find { it.title == 'Failed Process' }?.value == '`FAILED_PROCESS`'
        json.attachments[0].fields.find { it.title == 'Support' }?.value == 'contact@example.com'
    }

    def 'should use default values when map config has minimal settings'() {
        given:
        config = new SlackConfig([
            enabled: true,
            webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
            startMessage: [
                text: 'Starting...'
            ]
        ])
        messageBuilder = new SlackMessageBuilder(config, session)

        when:
        def message = messageBuilder.buildWorkflowStartMessage()
        def json = new JsonSlurper().parseText(message)

        then:
        json.attachments[0].text == 'Starting...'
        json.attachments[0].color == '#3AA3E3' // default INFO color
        json.attachments[0].fields.size() == 0 // no fields since includeFields not specified
    }
}
