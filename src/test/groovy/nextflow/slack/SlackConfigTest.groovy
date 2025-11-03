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

import nextflow.Session
import spock.lang.Specification

/**
 * Tests for SlackConfig
 */
class SlackConfigTest extends Specification {

    def 'should parse configuration from slack block'() {
        given:
        def session = Mock(Session)
        session.config >> [
            slack: [
                webhook: [
                    url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
                ],
                notifyOnStart: false,
                notifyOnComplete: true,
                notifyOnError: true
            ]
        ]

        when:
        def config = SlackConfig.from(session)

        then:
        config != null
        config.enabled == true
        config.webhook == 'https://hooks.slack.com/services/TEST/TEST/TEST'
        config.notifyOnStart == false
        config.notifyOnComplete == true
        config.notifyOnError == true
    }

    def 'should use default values when not specified'() {
        given:
        def session = Mock(Session)
        session.config >> [
            slack: [
                webhook: [
                    url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
                ]
            ]
        ]

        when:
        def config = SlackConfig.from(session)

        then:
        config != null
        config.enabled == true
        config.notifyOnStart == true
        config.notifyOnComplete == true
        config.notifyOnError == true
        config.includeCommandLine == true
        config.includeResourceUsage == true
    }

    def 'should return null when webhook is not configured'() {
        given:
        def session = Mock(Session)
        session.config >> [:]
        session.params >> [:]

        when:
        def config = SlackConfig.from(session)

        then:
        config == null
    }

    def 'should return null when explicitly disabled'() {
        given:
        def session = Mock(Session)
        session.config >> [
            slack: [
                webhook: [
                    url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
                ],
                enabled: false
            ]
        ]

        when:
        def config = SlackConfig.from(session)

        then:
        config == null
    }

    def 'isConfigured should return true when enabled and webhook set'() {
        given:
        def session = Mock(Session)
        session.config >> [
            slack: [
                webhook: [
                    url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
                ]
            ]
        ]

        when:
        def config = SlackConfig.from(session)

        then:
        config.isConfigured() == true
    }

    def 'createSender should return WebhookSlackSender for webhook configuration'() {
        given:
        def session = Mock(Session)
        session.config >> [
            slack: [
                webhook: [
                    url: 'https://hooks.slack.com/services/TEST/TEST/TEST'
                ]
            ]
        ]
        def config = SlackConfig.from(session)

        when:
        def sender = config.createSender()

        then:
        sender != null
        sender instanceof WebhookSlackSender
    }
}
