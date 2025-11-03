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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.Session

/**
 * Configuration parser for Slack plugin settings.
 *
 * Supports two integration types:
 * 1. Webhook: Uses Slack Incoming Webhooks (current implementation)
 * 2. Bot: Uses Slack Bot Token API (future implementation)
 *
 * Configuration structure:
 * slack {
 *     webhook {
 *         url = 'https://hooks.slack.com/services/...'
 *     }
 *     // Future: bot { token = 'xoxb-...', channel = '#workflows' }
 * }
 *
 * @author Adam Talbot <adam.talbot@seqera.io>
 */
@Slf4j
@CompileStatic
class SlackConfig {

    /**
     * Enable/disable the plugin
     */
    final boolean enabled

    /**
     * Slack webhook URL for posting messages (internal use)
     */
    final String webhook

    /**
     * Send notification when workflow starts
     */
    final boolean notifyOnStart

    /**
     * Send notification when workflow completes successfully
     */
    final boolean notifyOnComplete

    /**
     * Send notification when workflow errors
     */
    final boolean notifyOnError

    /**
     * Include command line in messages
     */
    final boolean includeCommandLine

    /**
     * Include resource usage in completion messages
     */
    final boolean includeResourceUsage

    /**
     * Custom message template for workflow start (simple string or map with full config)
     */
    final Object startMessage

    /**
     * Custom message template for workflow completion (simple string or map with full config)
     */
    final Object completeMessage

    /**
     * Custom message template for workflow error (simple string or map with full config)
     */
    final Object errorMessage

    /**
     * Private constructor - use from() factory method
     */
    private SlackConfig(Map config) {
        this.enabled = config.enabled != null ? config.enabled as boolean : true
        this.webhook = config.webhook as String
        this.notifyOnStart = config.notifyOnStart != null ? config.notifyOnStart as boolean : true
        this.notifyOnComplete = config.notifyOnComplete != null ? config.notifyOnComplete as boolean : true
        this.notifyOnError = config.notifyOnError != null ? config.notifyOnError as boolean : true
        this.includeCommandLine = config.includeCommandLine != null ? config.includeCommandLine as boolean : true
        this.includeResourceUsage = config.includeResourceUsage != null ? config.includeResourceUsage as boolean : true
        this.startMessage = config.startMessage ?: '🚀 *Pipeline started*'
        this.completeMessage = config.completeMessage ?: '✅ *Pipeline completed successfully*'
        this.errorMessage = config.errorMessage ?: '❌ *Pipeline failed*'
    }

    /**
     * Create SlackConfig from Nextflow session
     *
     * @param session The Nextflow session
     * @return SlackConfig instance, or null if not configured/disabled
     */
    static SlackConfig from(Session session) {
        // Build configuration map from session config
        def config = session.config?.navigate('slack') as Map ?: [:]

        // Check if explicitly disabled
        if (config.enabled == false) {
            log.debug "Slack plugin: Explicitly disabled in configuration"
            return null
        }

        // Get webhook URL from nested structure
        def webhook = getWebhookUrl(session)
        if (!webhook) {
            log.debug "Slack plugin: No webhook URL configured, plugin will be disabled"
            return null
        }

        // Set webhook string in config for constructor (replaces nested map)
        config.webhook = webhook

        def slackConfig = new SlackConfig(config)
        log.info "Slack plugin: Enabled with webhook notifications"
        return slackConfig
    }

    /**
     * Get webhook URL from config
     * Reads from nested webhook { url = '...' } structure
     */
    private static String getWebhookUrl(Session session) {
        return session.config?.navigate('slack.webhook.url') as String
    }


    /**
     * Check if plugin is configured and enabled
     */
    boolean isConfigured() {
        return enabled && webhook != null
    }

    /**
     * Create the appropriate SlackSender based on configuration
     *
     * @return SlackSender instance (WebhookSlackSender for now, future: BotSlackSender)
     */
    SlackSender createSender() {
        if (!isConfigured()) {
            throw new IllegalStateException("Cannot create sender: Slack plugin not configured")
        }

        // For now, only webhook sender is supported
        // Future: Check if bot config exists and create BotSlackSender
        return new WebhookSlackSender(webhook)
    }

    @Override
    String toString() {
        return "SlackConfig[enabled=${enabled}, webhook=${webhook ? '***configured***' : 'null'}, " +
               "notifyOnStart=${notifyOnStart}, " +
               "notifyOnComplete=${notifyOnComplete}, notifyOnError=${notifyOnError}]"
    }
}
