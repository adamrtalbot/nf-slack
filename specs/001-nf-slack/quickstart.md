# Quick Start Guide: nf-slack Plugin

**Feature**: nf-slack Notification Plugin
**Audience**: Nextflow users who want to add Slack notifications to their workflows
**Time to Complete**: 5 minutes

## Overview

This guide walks you through adding Slack notifications to any Nextflow workflow in three simple steps. By the end, you'll receive automatic notifications when your workflows start, complete, or fail.

## Prerequisites

- Nextflow installed (any recent version)
- Access to a Slack workspace where you can create webhooks
- An existing Nextflow workflow (or use `nextflow run hello` for testing)

## Step 1: Create a Slack Webhook (2 minutes)

1. Go to your Slack workspace in a web browser
2. Navigate to **Settings & administration** → **Manage apps**
3. Search for and select **Incoming Webhooks**
4. Click **Add to Slack**
5. Choose the channel where you want notifications (e.g., `#pipeline-alerts`)
6. Click **Add Incoming Webhooks Integration**
7. **Copy the Webhook URL** (looks like `https://hooks.slack.com/services/T.../B.../xxx...`)

**Security Tip**: Keep this URL secret - anyone with it can post to your Slack channel.

## Step 2: Configure the Plugin (1 minute)

Create or edit your `nextflow.config` file:

```groovy
plugins {
    id 'nf-slack@0.1.0'
}

slack {
    enabled = true
    webhook = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
}
```

**Replace** `'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'` with the webhook URL you copied in Step 1.

**That's it!** With just these lines, you'll get notifications for workflow start, completion, and errors.

## Step 3: Run Your Workflow (1 minute)

Run any Nextflow workflow as usual:

```bash
nextflow run main.nf
```

Or test with the built-in hello workflow:

```bash
nextflow run hello
```

**Check Slack**: You should see notifications appear in your configured channel!

## What You'll See

### When Workflow Starts

A blue notification with:
- Workflow name
- Run ID
- Start time
- Command line used

### When Workflow Completes

A green notification with:
- Workflow name
- Duration
- Task statistics (succeeded, failed, cached)
- Completion time

### When Workflow Fails

A red notification with:
- Workflow name
- Duration
- Error message
- Which process failed

## Next Steps

Now that basic notifications are working, you can explore advanced features:

### 1. Control Which Events Trigger Notifications

```groovy
slack {
    webhook = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

    // Only notify on errors
    notifyOnStart = false
    notifyOnComplete = false
    notifyOnError = true
}
```

### 2. Customize Notification Messages

```groovy
slack {
    webhook = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

    // Simple text customization
    startMessage = '🎬 My pipeline is starting!'
    completeMessage = '🎉 Analysis complete!'
    errorMessage = '💥 Something went wrong!'
}
```

### 3. Add Custom Fields

```groovy
slack {
    webhook = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

    // Advanced: Add custom fields
    completeMessage = [
        text: '✅ Analysis Complete',
        color: '#2EB887',
        customFields: [
            [title: 'Environment', value: 'Production', short: true],
            [title: 'Team', value: 'Bioinformatics', short: true]
        ]
    ]
}
```

### 4. Send Custom Messages from Workflows

You can also send custom notifications at specific points in your workflow:

```groovy
include { slackMessage } from 'plugin/nf-slack'

workflow {
    // Your workflow logic
    MY_PROCESS(input_ch)

    // Send custom notification
    slackMessage("✅ Critical step completed successfully")
}
```

Or with rich formatting:

```groovy
slackMessage([
    message: "QC Results",
    color: "#2EB887",
    fields: [
        [title: "Sample", value: "sample_001", short: true],
        [title: "Quality", value: "98.5%", short: true]
    ]
])
```

## Using Nextflow Secrets (Recommended)

Instead of putting the webhook URL directly in your config, use Nextflow secrets:

**1. Store the webhook securely:**

```bash
nextflow secrets set SLACK_WEBHOOK 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
```

**2. Reference it in your config:**

```groovy
slack {
    webhook = secrets.SLACK_WEBHOOK
}
```

This keeps the webhook URL out of version control and makes it easy to rotate if needed.

## Progressive Examples

The plugin includes 6 progressive example configurations showing different features:

1. **01-minimal.config** - Just the webhook (you are here!)
2. **02-notification-control.config** - Choose which events to notify
3. **03-message-text.config** - Customize message text
4. **04-message-colors.config** - Use custom colors
5. **05-custom-fields.config** - Add your own fields
6. **06-selective-fields.config** - Choose which default fields to show

Find them in `example/configs/` directory and see `example/configs/README.md` for detailed explanations.

## Troubleshooting

### No Messages Appearing in Slack?

**Check 1 - Webhook URL**: Verify the webhook URL is correct and starts with `https://hooks.slack.com/services/`

**Check 2 - Plugin Loaded**: Look for "Slack plugin:" messages in Nextflow logs:
```bash
nextflow run main.nf -log debug
```

**Check 3 - Test Webhook**: Verify your webhook works with curl:
```bash
curl -X POST -H 'Content-type: application/json' \
  --data '{"text":"Test message"}' \
  'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
```

### Messages Not Formatted Correctly?

- Make sure you're using valid hex color codes (e.g., `#2EB887`)
- Ensure field structures have required `title` and `value` fields
- Check that `short: true` fields come in pairs for proper layout

### Plugin Not Loading?

- Verify the plugin version exists: `nextflow plugin list`
- Check your Nextflow version is compatible (23.04.0+)
- Try installing explicitly: `nextflow plugin install nf-slack@0.1.0`

## Configuration Reference

### Basic Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `enabled` | Boolean | `true` | Enable/disable the plugin |
| `webhook` | String | Required | Slack webhook URL |
| `notifyOnStart` | Boolean | `true` | Send notification when workflow starts |
| `notifyOnComplete` | Boolean | `true` | Send notification when workflow completes |
| `notifyOnError` | Boolean | `true` | Send notification when workflow fails |

### Customization Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `username` | String | `"Nextflow Bot"` | Bot display name in Slack |
| `iconEmoji` | String | `":rocket:"` | Bot icon emoji |
| `channel` | String | (webhook default) | Override target channel |
| `startMessage` | String or Map | `"🚀 Pipeline started"` | Custom start message |
| `completeMessage` | String or Map | `"✅ Pipeline completed successfully"` | Custom completion message |
| `errorMessage` | String or Map | `"❌ Pipeline failed"` | Custom error message |

### Map-Based Message Format

When using map format for messages:

```groovy
[
    text: "Message title",                  // Required
    color: "#2EB887",                       // Optional hex color
    includeFields: ['field1', 'field2'],   // Optional: which default fields
    customFields: [                         // Optional: your custom fields
        [title: "Label", value: "Value", short: true]
    ]
]
```

**Available `includeFields`**:
- `runName` - The Nextflow run name
- `status` - Workflow status with emoji
- `duration` - How long the workflow ran
- `commandLine` - The command used
- `workDir` - Work directory
- `errorMessage` - Error details (error events only)
- `failedProcess` - Failed process name (error events only)
- `tasks` - Task statistics (completion events only)

## Getting Help

- **Documentation**: See the main README.md for comprehensive documentation
- **Examples**: Check `example/configs/` for working configuration examples
- **Issues**: Report problems at https://github.com/yourusername/nf-slack/issues
- **Questions**: Ask on Nextflow Slack workspace (#plugins channel)

## What's Next?

You now have Slack notifications working! Here are some ideas to explore:

1. **Customize for your team**: Add custom fields with project metadata
2. **Different channels**: Route errors to a different channel than completions
3. **Progressive examples**: Work through the 6 example configs to learn all features
4. **Custom messages**: Add fine-grained notifications at key workflow points
5. **Share with team**: Commit your `nextflow.config` (using secrets for the webhook!)

Happy workflow monitoring! 🚀
