# Quick Start

Get your first Slack notification working in minutes!

## Prerequisites

Before you begin, make sure you have:

- [x] A Slack webhook URL ([Installation guide](installation.md))
- [x] A Nextflow pipeline (v21.04.0 or later)
- [x] Basic familiarity with Nextflow configuration

## Step 1: Add the Plugin

Add the nf-slack plugin to your `nextflow.config`:

```groovy
plugins {
    id 'nf-slack@0.1.0'
}
```

!!! tip "Using Multiple Plugins?"
If you already have a `plugins` block, just add the nf-slack entry:

    ```groovy
    plugins {
        id 'nf-validation'
        id 'nf-slack@0.1.0'  // Add this line
    }
    ```

## Step 2: Configure the Webhook

Add the Slack configuration block with your webhook URL:

```groovy
slack {
    enabled = true
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }
}
```

!!! warning "Security Best Practice"
Don't hardcode your webhook URL! Use environment variables or Nextflow secrets instead:

    ```groovy
    slack {
        enabled = true
        webhook {
            url = env.SLACK_WEBHOOK_URL  // or secrets.SLACK_WEBHOOK_URL
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

Here's a complete minimal `nextflow.config`:

```groovy title="nextflow.config"
plugins {
    id 'nf-slack@0.1.0'
}

slack {
    enabled = true
    webhook {
        url = env.SLACK_WEBHOOK_URL
    }
}
```

And set your environment variable:

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

2. Run it:

```bash
nextflow run test.nf
```

3. Check your Slack channel for the start and completion notifications.

## Common Issues

### No Notifications Received

1. **Check the webhook URL** - Verify it's set correctly:

   ```bash
   echo $SLACK_WEBHOOK_URL
   ```

2. **Check plugin version** - Make sure you're using a valid version:

   ```groovy
   plugins {
       id 'nf-slack@0.1.0'  // Use the latest version
   }
   ```

3. **Check enabled flag** - Ensure Slack is enabled:
   ```groovy
   slack {
       enabled = true  // Must be true
   }
   ```

### Invalid Webhook URL

If you see errors about invalid webhook URLs, verify:

- The URL starts with `https://hooks.slack.com/`
- The URL hasn't been truncated
- The URL doesn't contain spaces or line breaks

## Next Steps

Now that you have basic notifications working, learn how to:

- [Customize automatic notifications](../usage/automatic-notifications.md)
- [Send custom messages from your workflow](../usage/custom-messages.md)
- [Configure advanced options](../usage/configuration.md)
- [View more examples](../examples/gallery.md)

## Need Help?

- Check the [Troubleshooting guide](../troubleshooting.md)
- Review the [API Reference](../reference/api.md)
- [Open an issue](https://github.com/adamrtalbot/nf-slack/issues) on GitHub
