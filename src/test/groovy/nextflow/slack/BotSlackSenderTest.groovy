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
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
class BotSlackSenderTest extends Specification {

    def 'should create bot sender with token and channel'() {
        when:
        def sender = new BotSlackSender('xoxb-test-token', 'C1234567890')

        then:
        sender.botToken == 'xoxb-test-token'
        sender.channelId == 'C1234567890'
    }

    def 'should handle send message without throwing exception'() {
        given:
        def sender = new BotSlackSender('xoxb-test-token', 'C1234567890')
        def message = '{"channel":"C1234567890","text":"Test message","blocks":[]}'

        when:
        sender.sendMessage(message)

        then:
        noExceptionThrown()
    }
}
