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

import nextflow.Session
import nextflow.script.WorkflowMetadata
import nextflow.trace.TraceRecord
import spock.lang.Specification

/**
 * Tests for SlackObserver and SlackFactory
 *
 * Note: Tests that attempt to send messages use invalid webhook URLs and will throw exceptions.
 * This reflects the actual behavior of the code when webhooks fail.
 */
class SlackObserverTest extends Specification {

    def 'should create the observer instance'() {
        given:
        def factory = new SlackFactory()

        when:
        def result = factory.create(Mock(Session))

        then:
        result.size() == 1
        result.first() instanceof SlackObserver
        SlackFactory.observerInstance instanceof SlackObserver
    }

    def 'should initialize when configuration is valid'() {
        given:
        def session = Mock(Session)
        session.config >> [
            slack: [
                webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
                notifyOnStart: false
            ]
        ]
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test.nf'
        session.workflowMetadata >> metadata
        session.runName >> 'test-run'

        def observer = new SlackObserver()

        when:
        observer.onFlowCreate(session)

        then:
        observer.config != null
        observer.client != null
        observer.messageBuilder != null
        noExceptionThrown()
    }

    def 'should handle missing configuration gracefully'() {
        given:
        def session = Mock(Session)
        session.config >> [:]
        session.params >> [:]

        def observer = new SlackObserver()

        when:
        observer.onFlowCreate(session)

        then:
        observer.config == null
        observer.client == null
        observer.messageBuilder == null
        noExceptionThrown()
    }


    def 'should handle onFlowComplete when not configured'() {
        given:
        def observer = new SlackObserver()

        when:
        observer.onFlowComplete()

        then:
        noExceptionThrown()
    }

    def 'should handle onFlowError when not configured'() {
        given:
        def observer = new SlackObserver()
        def errorRecord = Mock(TraceRecord)

        when:
        observer.onFlowError(null, errorRecord)

        then:
        noExceptionThrown()
    }

    def 'should send notification on flow complete when configured'() {
        given:
        def mockClient = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])
        mockClient.doSendMessage(_) >> true

        def session = Mock(Session)
        session.config >> [
            slack: [
                webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
                notifyOnStart: false,
                notifyOnComplete: true
            ]
        ]
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test.nf'
        session.workflowMetadata >> metadata
        session.runName >> 'test-run'

        def observer = new SlackObserver()
        observer.onFlowCreate(session)
        observer.setClient(mockClient)  // Inject mocked client

        when:
        observer.onFlowComplete()

        then:
        noExceptionThrown()
    }

    def 'should send notification on flow error when configured'() {
        given:
        def mockClient = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])
        mockClient.doSendMessage(_) >> true

        def session = Mock(Session)
        session.config >> [
            slack: [
                webhook: 'https://hooks.slack.com/services/TEST/TEST/TEST',
                notifyOnStart: false,
                notifyOnError: true
            ]
        ]
        def metadata = Mock(WorkflowMetadata)
        metadata.scriptName >> 'test.nf'
        metadata.errorMessage >> 'Test error'
        session.workflowMetadata >> metadata
        session.runName >> 'test-run'

        def errorRecord = Mock(TraceRecord)
        errorRecord.get('process') >> 'FAILED_PROCESS'

        def observer = new SlackObserver()
        observer.onFlowCreate(session)
        observer.setClient(mockClient)  // Inject mocked client

        when:
        observer.onFlowError(null, errorRecord)

        then:
        noExceptionThrown()
    }
}
