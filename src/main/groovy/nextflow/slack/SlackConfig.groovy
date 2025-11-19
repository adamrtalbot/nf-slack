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
 * 1. Bot: Uses Slack Bot Token API (recommended - more secure and capable)
 * 2. Webhook: Uses Slack Incoming Webhooks
 *
 * Note: If both bot and webhook are configured, bot takes precedence.
 *
 * Configuration structure:
 * slack {
 *     // Option 1: Bot authentication (recommended)
 *     bot {
 *         token = 'xoxb-...'       // Bot token from Slack app
 *         channel = 'C1234567890'  // Channel ID (not #name)
 *     }
 *
 *     // Option 2: Webhook (legacy)
 *     webhook {
 *         url = 'https://hooks.slack.com/services/...'
 *     }
 *
 *     // Event configuration (same for both modes)
 *     onStart {
 *         enabled = true
 *         message = '🚀 *Pipeline started*'
 *         includeCommandLine = true
 *     }
 *     onComplete {
 *         enabled = true
 *         message = '✅ *Pipeline completed*'
 *         includeCommandLine = true
 *         includeResourceUsage = true
 *     }
 *     onError {
 *         enabled = true
 *         message = '❌ *Pipeline failed*'
 *         includeCommandLine = true
 *     }
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
     * Slack bot token for API authentication (xoxb-* or xoxp-*)
     */
    final String botToken

    /**
     * Slack channel ID for bot messages (e.g., C1234567890)
     */
    final String botChannel

    /**
     * Configuration for workflow start notifications
     */
    final OnStartConfig onStart

    /**
     * Configuration for workflow completion notifications
     */
    final OnCompleteConfig onComplete

    /**
     * Configuration for workflow error notifications
     */
    final OnErrorConfig onError

    /**
     * Private constructor - use from() factory method
     */
    private SlackConfig(Map config) {
        this.enabled = config.enabled != null ? config.enabled as boolean : true
        this.webhook = config.webhook as String
        this.botToken = config.botToken as String
        this.botChannel = config.botChannel as String
        this.onStart = new OnStartConfig(config.onStart as Map)
        this.onComplete = new OnCompleteConfig(config.onComplete as Map)
        this.onError = new OnErrorConfig(config.onError as Map)
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

        // Get bot configuration
        def botToken = extractBotToken(session)
        def botChannel = extractBotChannel(session)

        // Get webhook URL from nested structure
        def webhook = extractWebhookUrl(session)

        // Determine which integration to use (bot takes precedence)
        if (botToken && botChannel) {
            // Validate bot token format
            if (!isValidBotToken(botToken)) {
                throw new IllegalArgumentException(
                    "Invalid Slack bot token format. Expected token starting with 'xoxb-' or 'xoxp-', got: ${botToken.take(10)}..."
                )
            }

            // Validate channel ID format
            if (!isValidChannelId(botChannel)) {
                throw new IllegalArgumentException(
                    "Invalid Slack channel ID format. Expected alphanumeric channel ID (e.g., 'C1234567890'), got: ${botChannel}. " +
                    "Note: Channel names (e.g., '#my-channel') are not supported. Please use the channel ID instead."
                )
            }

            config.botToken = botToken
            config.botChannel = botChannel

            def slackConfig = new SlackConfig(config)
            log.info "Slack plugin: Enabled with bot token authentication (channel: ${botChannel})"
            return slackConfig
        } else if (webhook) {
            // Fall back to webhook
            config.webhook = webhook

            def slackConfig = new SlackConfig(config)
            log.info "Slack plugin: Enabled with webhook notifications"
            return slackConfig
        } else {
            log.debug "Slack plugin: No bot or webhook configuration found, plugin will be disabled"
            return null
        }
    }

    /**
     * Extract webhook URL from config
     * Reads from nested webhook { url = '...' } structure
     */
    private static String extractWebhookUrl(Session session) {
        return session.config?.navigate('slack.webhook.url') as String
    }

    /**
     * Extract bot token from config
     * Reads from nested bot { token = '...' } structure
     */
    private static String extractBotToken(Session session) {
        return session.config?.navigate('slack.bot.token') as String
    }

    /**
     * Extract bot channel from config
     * Reads from nested bot { channel = '...' } structure
     */
    private static String extractBotChannel(Session session) {
        return session.config?.navigate('slack.bot.channel') as String
    }

    /**
     * Validate bot token format
     * Slack bot tokens should start with xoxb- (bot) or xoxp- (user)
     */
    private static boolean isValidBotToken(String token) {
        return token && (token.startsWith('xoxb-') || token.startsWith('xoxp-'))
    }

    /**
     * Validate channel ID format
     * Channel IDs are alphanumeric strings (e.g., C1234567890)
     * Channel names starting with # are NOT supported
     */
    private static boolean isValidChannelId(String channelId) {
        return channelId && channelId ==~ /^[A-Z0-9]+$/
    }

    /**
     * Check if plugin is configured and enabled
     */
    boolean isConfigured() {
        return enabled && (webhook != null || (botToken != null && botChannel != null))
    }

    /**
     * Check if bot mode is configured
     */
    boolean isBotMode() {
        return botToken != null && botChannel != null
    }

    /**
     * Get bot token (accessor for public API)
     */
    String getBotToken() {
        return botToken
    }

    /**
     * Get bot channel (accessor for public API)
     */
    String getBotChannel() {
        return botChannel
    }

    /**
     * Create the appropriate SlackSender based on configuration
     *
     * @return SlackSender instance (BotSlackSender or WebhookSlackSender)
     */
    SlackSender createSender() {
        if (!isConfigured()) {
            throw new IllegalStateException("Cannot create sender: Slack plugin not configured")
        }

        // Bot mode takes precedence
        if (isBotMode()) {
            return new BotSlackSender(botToken, botChannel)
        } else {
            return new WebhookSlackSender(webhook)
        }
    }

    @Override
    String toString() {
        def mode = isBotMode() ? "bot[channel=${botChannel}]" : (webhook ? 'webhook' : 'none')
        return "SlackConfig[enabled=${enabled}, mode=${mode}, " +
               "onStart=${onStart}, onComplete=${onComplete}, onError=${onError}]"
    }
}
