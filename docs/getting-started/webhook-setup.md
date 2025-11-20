# Slack Webhook Setup

This guide will walk you through setting up a Slack webhook for your workspace.

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

## Step 2: Enable Incoming Webhooks

1. In your app's settings, navigate to **"Incoming Webhooks"** in the sidebar
2. Toggle the switch to **"On"**
3. Click **"Add New Webhook to Workspace"**
4. Select the channel where you want notifications to appear
5. Click **"Allow"**

## Step 3: Copy Your Webhook URL

After authorization, you'll see your webhook URL. It will look like:

```
https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX
```

!!! warning "Keep Your Webhook URL Secret"

    This URL allows anyone to post messages to your Slack channel. Never commit it to version control or share it publicly.

## Step 4: Store Your Webhook URL Securely

You have several options for managing the webhook URL securely:

### Option 1: Environment Variable

_Recommended_

Set an environment variable:

```bash
export SLACK_WEBHOOK_URL='https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
```

Then reference it in your `nextflow.config`:

```groovy
slack {
    webhook {
        url = "$SLACK_WEBHOOK_URL"
    }
}
```

### Option 2: Nextflow Secret

_Better, but supported in fewer places_

Use [Nextflow secrets](https://www.nextflow.io/docs/latest/secrets.html):

```bash
nextflow secrets set SLACK_WEBHOOK_URL 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
```

Then reference it in your config:

```groovy
slack {
    webhook {
        url = secrets.SLACK_WEBHOOK_URL
    }
}
```

### Option 3: Configuration File

For testing only, you can put it directly in `nextflow.config`:

```groovy
slack {
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }
}
```

!!! danger "Don't Commit Webhook URLs"

    If you use this option, add your config file to `.gitignore` to prevent accidentally committing your webhook URL.

## Testing Your Webhook

To verify your webhook is working, test it using curl:

```bash
curl -X POST -H 'Content-type: application/json' \
  --data '{"text":"Test message from nf-slack"}' \
  $SLACK_WEBHOOK_URL
```

If this works, you should see a test message in your Slack channel.

## Next Steps

Now that you have your webhook URL configured, you can:

- Return to the [Quick Start guide](quick-start.md) to configure the plugin
- Learn about [automatic notifications](../usage/automatic-notifications.md)
- Explore [custom messages](../usage/custom-messages.md)

## Learn More

- [Slack Incoming Webhooks Documentation](https://api.slack.com/messaging/webhooks)
- [Nextflow Secrets Documentation](https://www.nextflow.io/docs/latest/secrets.html)
