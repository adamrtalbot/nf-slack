# Quick Start

Get your first Slack notification working in minutes!

## Prerequisites

Before you begin, make sure you have:

- [x] Slack authentication: either a [bot token](bot-setup.md) (recommended) or [webhook URL](webhook-setup.md)
- [x] A Nextflow pipeline (v24.10.0 or later)
- [x] Basic familiarity with Nextflow configuration

## Step 1: Add the Plugin

Add the nf-slack plugin to your `nextflow.config`:

```groovy
plugins {
    id 'nf-slack@0.2.1'
}
```

!!! tip "Using Multiple Plugins?"

    If you already have a `plugins` block, just add the nf-slack entry:

    ```groovy
    plugins {
        id 'nf-validation'
        id 'nf-slack@0.2.1'  // Add this line
    }
    ```

## Step 2: Configure Authentication

Choose one of the two authentication methods:

### Option 1: Bot Authentication (Recommended)

Bot authentication provides better security and flexibility. See the [Bot Setup Guide](bot-setup.md) for detailed instructions on creating a Slack app and getting your bot token.

```groovy
slack {
    enabled = true
    bot {
        token = System.getenv("SLACK_BOT_TOKEN")
        channel = System.getenv("SLACK_CHANNEL_ID")
    }
}
```

### Option 2: Webhook Authentication

Alternatively, you can use a webhook URL. See the [Webhook Setup Guide](webhook-setup.md) for webhook setup instructions.

```groovy
slack {
    enabled = true
    webhook {
        url = System.getenv("SLACK_WEBHOOK_URL")
    }
}
```

!!! warning "Security Best Practice"

    Never hardcode credentials! Always use environment variables or Nextflow secrets:

    ```groovy
    slack {
        enabled = true
        bot {
            token = "$SLACK_BOT_TOKEN"  // or secrets.SLACK_BOT_TOKEN
            channel = "$SLACK_CHANNEL_ID"
        }
    }
    ```

## Step 3: Run Your Pipeline

That's it! Run your pipeline normally:

```bash
nextflow run main.nf
```

You'll receive Slack notifications when your pipeline:

- 🚀 Starts
- ✅ Completes successfully
- ❌ Fails

![Default notifications](../images/nf-slack-00.png)

## Next Steps

Now that you have basic notifications working, learn how to:

- [Customize automatic notifications](../usage/automatic-notifications.md)
- [Send custom messages from your workflow](../usage/custom-messages.md)
- [Configure advanced options](../usage/configuration.md)
- [View more examples](../examples/gallery.md)

## Need Help?

- Review the [API Reference](../reference/api.md)
- [Open an issue](https://github.com/adamrtalbot/nf-slack/issues) on GitHub
