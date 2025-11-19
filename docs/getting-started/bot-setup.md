# Slack Bot Setup Guide

This guide walks you through setting up Slack Bot authentication for the nf-slack plugin. Bot authentication is the **recommended method** as it's more secure, more capable, and easier to manage than webhooks.

## Why Use Bot Authentication?

Bot authentication offers several advantages over webhooks:

- **More secure**: Token-based authentication with fine-grained permissions
- **Better control**: Revoke or rotate tokens without creating new webhook URLs
- **More features**: Access to additional Slack APIs (future enhancements)
- **Audit trail**: See bot actions in Slack's audit logs
- **No URL management**: No need to store or manage webhook URLs

## Prerequisites

- A Slack workspace where you have permission to install apps
- Admin access to create Slack apps (or request one from your workspace admin)

## Step 1: Create a Slack App

1. Go to [https://api.slack.com/apps](https://api.slack.com/apps)
2. Click **"Create New App"**
3. Select **"From scratch"**
4. Enter:
   - **App Name**: e.g., "Nextflow Notifications"
   - **Workspace**: Select your workspace
5. Click **"Create App"**

## Step 2: Add Bot Permissions

1. In your app's settings, navigate to **"OAuth & Permissions"** in the left sidebar
2. Scroll down to **"Scopes"** → **"Bot Token Scopes"**
3. Click **"Add an OAuth Scope"** and add:
   - `chat:write` - Required to send messages
   - `chat:write.public` - Optional, allows posting to channels without joining them first

## Step 3: Install the App to Your Workspace

1. Still in **"OAuth & Permissions"**, scroll to the top
2. Click **"Install to Workspace"**
3. Review the permissions and click **"Allow"**
4. You'll see a **"Bot User OAuth Token"** starting with `xoxb-`
5. **Copy this token** - you'll need it for configuration

## Step 4: Get the Channel ID

You need the channel ID (not the channel name) for configuration:

### Method 1: Via Slack App

1. Open Slack and navigate to the channel where you want notifications
2. Click the channel name at the top
3. Scroll down in the "About" tab
4. At the bottom, you'll see the **Channel ID** (e.g., `C1234567890`)
5. Copy this ID

### Method 2: Via Slack URL

1. Open the channel in your web browser
2. Look at the URL: `https://app.slack.com/client/T.../C1234567890`
3. The part starting with `C` is your channel ID

## Step 5: Configure nf-slack

Add the bot configuration to your `nextflow.config`:

```groovy
plugins {
    id 'nf-slack'
}

slack {
    bot {
        token = 'xoxb-your-bot-token-here'
        channel = 'C1234567890'  // Your channel ID
    }
}
```

### Secure Token Management

**Never commit tokens directly to your config files!** Use one of these approaches:

#### Option 1: Environment Variables

```groovy
slack {
    bot {
        token = System.getenv('SLACK_BOT_TOKEN')
        channel = System.getenv('SLACK_CHANNEL_ID')
    }
}
```

Then set environment variables:

```bash
export SLACK_BOT_TOKEN='xoxb-your-bot-token-here'
export SLACK_CHANNEL_ID='C1234567890'
```

#### Option 2: Secrets File

Create a `secrets.config` file (add to `.gitignore`):

```groovy
slack {
    bot {
        token = 'xoxb-your-bot-token-here'
        channel = 'C1234567890'
    }
}
```

Include it in your main config:

```groovy
includeConfig 'secrets.config'
```

#### Option 3: Nextflow Secrets

Use Nextflow's built-in secrets management:

```bash
nextflow secrets set SLACK_BOT_TOKEN xoxb-your-bot-token-here
nextflow secrets set SLACK_CHANNEL_ID C1234567890
```

Then in your config:

```groovy
slack {
    bot {
        token = secrets.SLACK_BOT_TOKEN
        channel = secrets.SLACK_CHANNEL_ID
    }
}
```

## Step 6: Test Your Configuration

Run a simple Nextflow workflow to test:

```bash
nextflow run hello -with-notification
```

You should see a notification in your Slack channel!

## Troubleshooting

### "Invalid bot token format" Error

Make sure your token starts with `xoxb-` (bot token) or `xoxp-` (user token). The token should be copied exactly from the Slack app settings.

### "Invalid channel ID format" Error

The plugin requires a channel ID (e.g., `C1234567890`), not a channel name (e.g., `#my-channel`). Follow Step 4 above to get the correct ID.

### Bot Can't Post to Channel

Make sure:

1. The bot has been invited to the channel (type `/invite @YourBotName` in the channel)
2. Or, you've added the `chat:write.public` permission to post without being in the channel

### Messages Not Appearing

Check that:

1. The token is correctly set (no extra spaces or quotes)
2. The channel ID is correct
3. Your Nextflow version is compatible (24.10.0+)
4. Check Nextflow logs for any error messages

## Migrating from Webhooks

If you're currently using webhooks, migration is simple:

1. Complete Steps 1-4 above to get your bot token and channel ID
2. Update your `nextflow.config`:

```groovy
// Before (webhook)
slack {
    webhook {
        url = 'https://hooks.slack.com/services/...'
    }
}

// After (bot)
slack {
    bot {
        token = 'xoxb-...'
        channel = 'C1234567890'
    }
}
```

3. Test your configuration
4. Once confirmed working, you can deactivate the old webhook

**Note**: If both bot and webhook configurations are present, bot will be used (it takes precedence).

## Next Steps

- [Configuration Reference](../usage/configuration.md) - Customize notification behavior
- [Automatic Notifications](../usage/automatic-notifications.md) - Configure what events trigger notifications
- [Custom Messages](../usage/custom-messages.md) - Send custom messages from your workflow

## Security Best Practices

1. **Never commit tokens** to version control
2. **Use environment variables** or secrets management
3. **Rotate tokens** regularly (can be done in Slack app settings)
4. **Limit bot permissions** to only what's needed (just `chat:write` is sufficient)
5. **Use separate bots** for dev/staging/production environments
