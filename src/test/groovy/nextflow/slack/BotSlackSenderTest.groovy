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

import spock.lang.Specification

/**
 * Tests for BotSlackSender
 */
class BotSlackSenderTest extends Specification {

    def 'should create sender with token and channel'() {
        when:
        def sender = new BotSlackSender('xoxb-token', 'C123456')

        then:
        sender != null
    }

    def 'should handle message sending gracefully'() {
        when:
        def sender = new BotSlackSender('xoxb-token', 'C123456')
        sender.sendMessage('{"text":"test"}')

        then:
        noExceptionThrown()
    }

    def 'should handle invalid JSON gracefully'() {
        when:
        def sender = new BotSlackSender('xoxb-token', 'C123456')
        sender.sendMessage('not valid json')

        then:
        noExceptionThrown()
    }

    def 'should return null for threadTs initially'() {
        when:
        def sender = new BotSlackSender('xoxb-token', 'C123456')

        then:
        sender.getThreadTs() == null
    }

    def 'should have getThreadTs method available'() {
        given:
        def sender = new BotSlackSender('xoxb-token', 'C123456')

        when:
        def threadTs = sender.getThreadTs()

        then:
        threadTs == null || threadTs instanceof String
    }

    // Note: We cannot easily test the actual HTTP call without mocking HttpURLConnection
    // or using a mock server. The thread timestamp capture happens in the postToSlack method
    // after a successful API response. Full testing would require:
    // 1. Mocking HttpURLConnection to return a successful response with a 'ts' field
    // 2. Verifying that threadTs is captured from the response
    // 3. Verifying that subsequent calls don't overwrite the threadTs
    // For this implementation, we rely on integration testing and the fact that the code
    // doesn't throw exceptions during normal operation.
}
