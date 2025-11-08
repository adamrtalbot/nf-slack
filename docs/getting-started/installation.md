# Installation

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

### Option 1: Environment Variable (Recommended)

Set an environment variable:

```bash
export SLACK_WEBHOOK_URL='https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
```

Then reference it in your `nextflow.config`:

```groovy
slack {
    webhook {
        url = env.SLACK_WEBHOOK_URL
    }
}
```

### Option 2: Nextflow Secrets

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

### Option 3: Configuration File (Not Recommended for Production)

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

## Next Steps

Now that you have your webhook URL, proceed to the [Quick Start guide](quick-start.md) to configure the plugin.

## Troubleshooting

### I Don't See "Incoming Webhooks" in My App Settings

Make sure you're creating a new app, not using an existing one. Older Slack apps may have different configuration options.

### My Webhook URL Doesn't Work

- Verify the URL is complete and hasn't been truncated
- Test it using curl:

```bash
curl -X POST -H 'Content-type: application/json' \
  --data '{"text":"Test message from nf-slack"}' \
  YOUR_WEBHOOK_URL
```

If this works, the issue is with your Nextflow configuration.

### I Need to Change the Channel

You can create a new webhook for a different channel, or edit the existing webhook in your Slack app settings.

## Learn More

- [Slack Incoming Webhooks Documentation](https://api.slack.com/messaging/webhooks)
- [Nextflow Secrets Documentation](https://www.nextflow.io/docs/latest/secrets.html)
