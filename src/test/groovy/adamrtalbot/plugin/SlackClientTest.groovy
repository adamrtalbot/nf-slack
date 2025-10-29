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

/**
 * Tests for SlackClient
 *
 * These tests mock HTTP connections to test error handling and logging behavior
 * without making actual network calls.
 */
class SlackClientTest extends Specification {

    def 'should create client with webhook URL'() {
        when:
        def client = new SlackClient('https://hooks.slack.com/services/TEST/TEST/TEST')

        then:
        client != null
    }

    def 'should send message successfully with mocked HTTP connection'() {
        given:
        def client = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])
        def message = '{"text":"test message"}'

        // Mock the doSendMessage to simulate successful send
        client.doSendMessage(_) >> { /* do nothing - simulate success */ }

        when:
        client.sendMessage(message)

        then:
        noExceptionThrown()

        cleanup:
        client.shutdown()
    }

    def 'should handle HTTP 404 error response'() {
        given:
        def client = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])
        def message = '{"text":"test message"}'

        // Mock doSendMessage to throw RuntimeException for 404
        client.doSendMessage(_) >> { throw new RuntimeException("Slack webhook HTTP 404: Not Found") }

        when:
        client.sendMessage(message)

        then:
        def exception = thrown(RuntimeException)
        exception.message.contains("404")
        exception.message.contains("Not Found")

        cleanup:
        client.shutdown()
    }

    def 'should handle HTTP 500 error response'() {
        given:
        def client = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])
        def message = '{"text":"test message"}'

        // Mock doSendMessage to throw RuntimeException for 500
        client.doSendMessage(_) >> { throw new RuntimeException("Slack webhook HTTP 500: Internal Server Error") }

        when:
        client.sendMessage(message)

        then:
        def exception = thrown(RuntimeException)
        exception.message.contains("500")

        cleanup:
        client.shutdown()
    }

    def 'should handle connection timeout'() {
        given:
        def client = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])
        def message = '{"text":"test message"}'

        // Mock doSendMessage to throw SocketTimeoutException
        client.doSendMessage(_) >> { throw new RuntimeException("Connection timeout") }

        when:
        client.sendMessage(message)

        then:
        def exception = thrown(RuntimeException)
        exception.message.contains("timeout")

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

    def 'should shutdown gracefully even with pending messages'() {
        given:
        def client = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])

        // Mock to simulate successful send
        client.doSendMessage(_) >> { Thread.sleep(50) } // Simulate some processing time

        when:
        client.sendMessage('{"text":"test"}')
        client.shutdown()

        then:
        noExceptionThrown()
    }

    def 'sendMessageWithRetry should return true on success'() {
        given:
        def client = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])
        def message = '{"text":"test message"}'

        // Mock doSendMessage to return true (success)
        client.doSendMessage(_) >> true

        when:
        def result = client.sendMessageWithRetry(message)

        then:
        result == true

        cleanup:
        client.shutdown()
    }

    def 'sendMessageWithRetry should return false after max retries'() {
        given:
        def client = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])
        def message = '{"text":"test message"}'

        // Mock doSendMessage to always return false (failure)
        client.doSendMessage(_) >> false

        when:
        def result = client.sendMessageWithRetry(message)

        then:
        result == false

        cleanup:
        client.shutdown()
    }

    def 'sendMessageWithRetry should retry on failure then succeed'() {
        given:
        def client = Spy(SlackClient, constructorArgs: ['https://hooks.slack.com/services/TEST/TEST/TEST'])
        def message = '{"text":"test message"}'
        def callCount = 0

        // Mock to fail twice then succeed
        client.doSendMessage(_) >> {
            callCount++
            return callCount >= 3 // Succeed on third attempt
        }

        when:
        def result = client.sendMessageWithRetry(message)

        then:
        result == true
        callCount == 3

        cleanup:
        client.shutdown()
    }
}
