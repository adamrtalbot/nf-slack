# Slack Bot Setup

This guide will walk you through setting up a Slack App and Bot User for your workspace. This is the recommended method for integrating with Slack as it offers better security and granular permissions.

## Prerequisites

- A Slack workspace where you have permission to add apps
- Access to your Nextflow pipeline configuration

## Step 1: Create a Slack App

1. Go to [Slack API Apps](https://api.slack.com/apps)
2. Click **"Create New App"**
3. Choose **"From scratch"**
4. Give your app a name (e.g., "Nextflow Notifications")
5. Select your workspace
6. Click **"Create App"**

## Step 2: Add Bot Permissions

1. In your app's settings, navigate to **"OAuth & Permissions"** in the sidebar
2. Scroll down to **"Scopes"** section
3. Under **"Bot Token Scopes"**, click **"Add an OAuth Scope"**
4. Add the `chat:write` scope (allows the bot to send messages)

## Step 3: Install App to Workspace

1. Scroll up to **"OAuth Tokens for Your Workspace"**
2. Click **"Install to Workspace"**
3. Click **"Allow"** to authorize the app

## Step 4: Copy Your Bot Token

After installation, you'll see your **Bot User OAuth Token**. It starts with `xoxb-`.

```
xoxb-your-token-here
```

!!! warning "Keep Your Token Secret"

    This token allows access to your Slack workspace. Never commit it to version control or share it publicly.

## Step 5: Invite Bot to Channel

1. Go to the Slack channel where you want notifications
2. Type `/invite @YourAppName` (replace with your bot's name)
3. Press Enter

## Step 6: Configure Nextflow

You need the **Bot Token** and the **Channel ID**.

### Finding the Channel ID

1. Right-click the channel name in Slack
2. Select **"Copy Link"**
3. The link looks like `https://.../archives/C12345678`
4. The last part (`C12345678`) is your Channel ID

### Configuration

Add to your `nextflow.config`:

```groovy
slack {
    enabled = true
    bot {
        token = secrets.SLACK_BOT_TOKEN
        channel = 'C12345678'
    }
}
```

!!! tip "Use Secrets"
Store your token as a Nextflow secret:
`nextflow secrets set SLACK_BOT_TOKEN 'xoxb-...'`

## Next Steps

- Return to the [Quick Start guide](quick-start.md)
- Learn about [automatic notifications](../usage/automatic-notifications.md)
