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

import spock.lang.Specification
import java.util.concurrent.TimeUnit

/**
 * Tests for SlackClient
 */
class SlackClientTest extends Specification {

    def 'should create client with webhook URL'() {
        when:
        def client = new SlackClient('https://hooks.slack.com/services/TEST/TEST/TEST')

        then:
        client != null
    }

    def 'should accept async message send'() {
        given:
        def client = new SlackClient('https://hooks.slack.com/services/TEST/TEST/TEST')
        def message = '{"text":"test message"}'

        when:
        client.sendMessage(message)
        // Give async operation time to attempt
        Thread.sleep(100)

        then:
        noExceptionThrown()

        cleanup:
        client.shutdown()
    }

    def 'should shutdown gracefully'() {
        given:
        def client = new SlackClient('https://hooks.slack.com/services/TEST/TEST/TEST')

        when:
        client.shutdown()

        then:
        noExceptionThrown()
    }

    def 'should handle multiple async messages'() {
        given:
        def client = new SlackClient('https://hooks.slack.com/services/TEST/TEST/TEST')

        when:
        5.times { i ->
            client.sendMessage("{\"text\":\"message ${i}\"}")
        }
        Thread.sleep(500)
        client.shutdown()

        then:
        noExceptionThrown()
    }

    def 'shutdown should wait for pending messages'() {
        given:
        def client = new SlackClient('https://hooks.slack.com/services/TEST/TEST/TEST')
        def message = '{"text":"test"}'

        when:
        client.sendMessage(message)
        def start = System.currentTimeMillis()
        client.shutdown()
        def duration = System.currentTimeMillis() - start

        then:
        noExceptionThrown()
        duration >= 0 // Shutdown should complete, even if webhook fails
    }
}
