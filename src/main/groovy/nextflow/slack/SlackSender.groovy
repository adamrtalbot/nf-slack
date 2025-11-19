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

/**
 * Interface for sending messages to Slack.
 *
 * Implementations include:
 * - WebhookSlackSender: Sends messages via Slack Incoming Webhooks
 * - BotSlackSender: Sends messages via Slack Bot Token API
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
interface SlackSender {

    /**
     * Send a message to Slack
     *
     * @param message JSON message payload
     */
    void sendMessage(String message)
}
