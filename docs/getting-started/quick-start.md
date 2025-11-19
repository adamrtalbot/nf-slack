# Quick Start

Get your first Slack notification working in minutes!

## Prerequisites

Before you begin, make sure you have:

- [x] Slack authentication: either a [bot token](bot-setup.md) (recommended) or [webhook URL](installation.md)
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

### Option 1: Bot Authentication (Recommended)

Add the Slack configuration block with your bot token and channel ID:

```groovy
slack {
    enabled = true
    bot {
        token = "$SLACK_BOT_TOKEN"
        channel = "$SLACK_CHANNEL_ID"
    }
}
```

!!! tip "Why Bot Authentication?"

    Bot authentication is more secure and capable than webhooks:

    - **Secure**: Tokens can be rotated and have granular permissions
    - **Flexible**: Post to any channel the bot has access to
    - **Traceable**: Messages show as coming from your bot

    See the [Bot Setup Guide](bot-setup.md) for detailed setup instructions.

### Option 2: Webhook Authentication (Legacy)

Alternatively, you can use a webhook URL:

```groovy
slack {
    enabled = true
    webhook {
        url = "$SLACK_WEBHOOK_URL"
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

## Complete Minimal Example

### Using Bot Authentication (Recommended)

Here's a complete minimal `nextflow.config`:

```groovy title="nextflow.config"
plugins {
    id 'nf-slack@0.2.1'
}

slack {
    bot {
        token = "$SLACK_BOT_TOKEN"
        channel = "$SLACK_CHANNEL_ID"
    }
}
```

Set your environment variables:

```bash
export SLACK_BOT_TOKEN='xoxb-your-bot-token'
export SLACK_CHANNEL_ID='C1234567890'
nextflow run main.nf
```

### Using Webhook (Legacy)

```groovy title="nextflow.config"
plugins {
    id 'nf-slack@0.2.1'
}

slack {
    webhook {
        url = "$SLACK_WEBHOOK_URL"
    }
}
```

```bash
export SLACK_WEBHOOK_URL='https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
nextflow run main.nf
```

## Verify It's Working

To test your configuration without running a full pipeline:

1. Create a simple test workflow:

   ```groovy title="test.nf"
   workflow {
       println "Testing nf-slack notifications!"
   }
   ```

1. Run it:

   ```bash
   nextflow run test.nf
   ```

1. Check your Slack channel for the start and completion notifications.

## Next Steps

Now that you have basic notifications working, learn how to:

- [Customize automatic notifications](../usage/automatic-notifications.md)
- [Send custom messages from your workflow](../usage/custom-messages.md)
- [Configure advanced options](../usage/configuration.md)
- [View more examples](../examples/gallery.md)

## Need Help?

- Review the [API Reference](../reference/api.md)
- [Open an issue](https://github.com/adamrtalbot/nf-slack/issues) on GitHub
