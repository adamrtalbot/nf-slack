# nf-slack plugin

A Nextflow plugin for sending Slack notifications during workflow execution.

## Features

- 🚀 **Automatic Notifications**: Get notified when workflows start, complete, or fail
- 💬 **Custom Messages**: Send custom messages from within your workflow scripts
- 🎨 **Rich Formatting**: Beautiful Slack messages with Slack Block Kit formatting
- ⚙️ **Highly Configurable**: Control what notifications are sent and when
- 🛡️ **Fail-Safe**: Never fails your workflow, even if Slack is unavailable
- 🔒 **Secure**: Keep webhook URLs in config files or use Nextflow secrets

## Current Limitations

- Only supports Slack Incoming Webhooks (no Bot API yet)
- Threads are not supported

## Quick Start

### 1. Set up a Slack Webhook

1. Go to your Slack workspace settings
2. Navigate to "Apps" → "Incoming Webhooks"
3. Create a new webhook and copy the URL

### 2. Add the Plugin to Your Pipeline

In your `nextflow.config`:

```groovy
plugins {
    id 'nf-slack@0.1.0'
}

slack {
    enabled = true

    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }
}
```

> **Note**: All notifications (start, complete, error) are enabled by default. See [Configuration](#configuration) to customize.

### 3. Run Your Pipeline

That's it! You'll now receive Slack notifications for your workflow events.

## 📚 Configuration Examples

We provide **6 progressive examples** demonstrating plugin features from basic to advanced:

| Example                                                                          | Feature                | Description                               |
| -------------------------------------------------------------------------------- | ---------------------- | ----------------------------------------- |
| [01-minimal.config](example/configs/01-minimal.config)                           | Enable notifications   | Just webhook, use defaults                |
| [02-notification-control.config](example/configs/02-notification-control.config) | Control when to notify | Choose which events trigger notifications |
| [03-message-text.config](example/configs/03-message-text.config)                 | Customize message text | Change notification text with strings     |
| [04-message-colors.config](example/configs/04-message-colors.config)             | Customize colors       | Use custom colors (map format)            |
| [05-custom-fields.config](example/configs/05-custom-fields.config)               | Add custom fields      | Include your own information              |
| [06-selective-fields.config](example/configs/06-selective-fields.config)         | Choose default fields  | Select which workflow info to show        |

Each example focuses on **one specific feature** and builds upon the previous ones.

**[View all examples with detailed documentation →](example/configs/README.md)**

## Configuration

> **📖 For complete API reference, see [Configuration Reference](docs/CONFIG.md)**

### Basic Setup

```groovy
plugins {
    id 'nf-slack@0.1.0'
}

slack {
    enabled = true

    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }
}
```

### Notification Control

Control which events trigger Slack notifications. All are enabled by default.

```groovy
slack {
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }

    // Enable or disable specific notifications
    onStart {
        enabled = true    // Send notification when workflow starts
    }

    onComplete {
        enabled = true    // Send notification when workflow completes
    }

    onError {
        enabled = true    // Send notification when workflow fails
    }
}
```

### Message Customization

You can customize notification messages in two ways: **simple text** or **rich formatted messages**.

#### Simple Text Messages

Use strings for quick customization (supports Slack markdown):

```groovy
slack {
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }

    onStart {
        message = '🎬 *My analysis pipeline is starting!*'
    }

    onComplete {
        message = '🎉 *Analysis completed successfully!*'
    }

    onError {
        message = '💥 *Pipeline encountered an error!*'
    }
}
```

**Default messages** (if not customized):

- **Start**: `🚀 *Pipeline started*`
- **Complete**: `✅ *Pipeline completed successfully*`
- **Error**: `❌ *Pipeline failed*`

#### Rich Formatted Messages

For full control over colors, fields, and layout:

```groovy
slack {
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }

    onStart {
        message = [
            text: '🚀 *Production Pipeline Starting*',
            color: '#3AA3E3',  // Custom blue color
            includeFields: ['runName', 'status', 'commandLine'],
            customFields: [
                [title: 'Environment', value: 'Production', short: true],
                [title: 'Priority', value: 'High', short: true]
            ]
        ]
    }

    onComplete {
        message = [
            text: '✅ *Analysis Complete*',
            color: '#2EB887',  // Green
            includeFields: ['runName', 'duration', 'status', 'tasks'],
            customFields: [
                [title: 'Results Location', value: 's3://my-bucket/results/', short: false]
            ]
        ]
    }

    onError {
        message = [
            text: '❌ *Pipeline Failed*',
            color: '#A30301',  // Red
            includeFields: ['runName', 'duration', 'errorMessage', 'failedProcess'],
            customFields: [
                [title: 'Support', value: 'support@example.com', short: true]
            ]
        ]
    }
}
```

**Available color codes:**

- **Success**: `#2EB887` (green)
- **Error**: `#A30301` (red)
- **Info**: `#3AA3E3` (blue)

**Available includeFields options:**

- `runName` - The Nextflow run name
- `status` - Workflow status with emoji
- `duration` - How long the workflow ran _(not available for start messages)_
- `commandLine` - The command used to launch the workflow
- `workDir` - The work directory _(only for start messages)_
- `errorMessage` - Error details _(only for error messages)_
- `failedProcess` - Which process failed _(only for error messages)_
- `tasks` - Task statistics _(only for completion messages)_

**Custom field structure:**

- `title` (required) - Field label
- `value` (required) - Field content
- `short` (optional) - If `true`, field appears in column layout (default: `false`)

### Per-Notification Settings

You can control what information is included in each notification type:

```groovy
slack {
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }

    onStart {
        enabled = true
        includeCommandLine = true   // Include command line in start messages
    }

    onComplete {
        enabled = true
        includeCommandLine = true       // Include command line in completion messages
        includeResourceUsage = true     // Include task statistics (onComplete only)
    }

    onError {
        enabled = true
        includeCommandLine = true   // Include command line in error messages
    }
}
```

> **Note**: `includeResourceUsage` is only available for `onComplete` notifications.

### Disabling the Plugin

```groovy
slack {
    enabled = false
}
```

Or simply don't configure a webhook - the plugin will disable itself if no webhook URL is configured.

## Custom Messages from Workflows

You can send custom messages from within your workflow scripts. The plugin must be enabled and configured for custom messages to work.

### Simple Text Messages

```groovy
include { slackMessage } from 'plugin/nf-slack'

workflow {
    slackMessage("🔬 Starting analysis for sample ${params.sample_id}")

    // Your workflow logic here
    MY_PROCESS(input_ch)

    slackMessage("✅ Analysis complete!")
}
```

### Rich Formatted Messages

```groovy
include { slackMessage } from 'plugin/nf-slack'

workflow {
    slackMessage([
        message: "Analysis Results",
        color: "#2EB887",  // green
        fields: [
            [title: "Sample", value: params.sample_id, short: true],
            [title: "Status", value: "Success", short: true],
            [title: "Total Variants", value: "1,234", short: true],
            [title: "Duration", value: "2h 30m", short: true]
        ]
    ])
}
```

### Message Field Structure

Each field in the `fields` array can have:

- `title` (required): Field label
- `value` (required): Field content
- `short` (optional): If `true`, field appears in a column layout (default: `false`)

## Example Use Cases

### Send Results Summary

```groovy
include { slackMessage } from 'plugin/nf-slack'

workflow {
    ANALYZE_DATA(input_ch)

    ANALYZE_DATA.out.results
        .map { sample, vcf, stats ->
            slackMessage([
                message: "Sample ${sample} analyzed",
                color: "#2EB887",
                fields: [
                    [title: "Sample", value: sample, short: true],
                    [title: "Variants", value: stats.variant_count, short: true],
                    [title: "Quality", value: stats.mean_quality, short: true]
                ]
            ])
        }
}
```

### Error Notifications with Context

```groovy
include { slackMessage } from 'plugin/nf-slack'

process CRITICAL_STEP {
    errorStrategy 'ignore'

    input:
    val sample_id

    script:
    """
    if ! run_critical_analysis.sh ${sample_id}; then
        slackMessage([
            message: "❌ Critical step failed for ${sample_id}",
            color: "#A30301",
            fields: [
                [title: "Sample", value: "${sample_id}", short: true],
                [title: "Process", value: "CRITICAL_STEP", short: true]
            ]
        ])
        exit 1
    fi
    """
}
```

## Message Format

The plugin sends messages using Slack's attachment format with Block Kit elements:

- **Workflow Start**: Blue color, includes run name, session ID, command line, and work directory
- **Workflow Complete**: Green color, includes duration, task counts, and resource usage
- **Workflow Error**: Red color, includes error message, failed process, and command line

All messages include:

- Workflow name as the author
- Nextflow icon
- Timestamp footer
- Configurable bot username and icon

## Troubleshooting

### No Messages Received

1. **Check webhook URL**: Ensure it's valid and starts with `https://hooks.slack.com`
2. **Check logs**: Look for "Slack plugin:" messages in the Nextflow log
3. **Test webhook**: Use curl to test your webhook directly:
   ```bash
   curl -X POST -H 'Content-type: application/json' \
     --data '{"text":"Test message"}' \
     YOUR_WEBHOOK_URL
   ```

### Messages Not Formatted Correctly

- Ensure you're using the correct field structure for rich messages
- Check that color codes are in hex format (e.g., `#2EB887`)
- Verify that `short: true` fields come in pairs for proper layout

### Plugin Not Loading

1. Verify plugin is installed: `nextflow plugin list`
2. Check version in config matches installed version
3. Try reinstalling: `make clean && make install`

### Rate Limiting

The plugin includes built-in rate limiting (max 1 message per second) and retry logic. If you're hitting Slack rate limits:

- Reduce the frequency of custom messages
- Consider batching notifications
- Check Slack workspace limits

## Development

### Build the Plugin

```bash
make assemble
```

### Run Tests

```bash
make test
```

### Install Locally

```bash
make install
```

### Test with Nextflow

```bash
nextflow run hello -plugins nf-slack@0.1.0
```

For information on publishing the plugin and contributing, see [CONTRIBUTING.md](docs/CONTRIBUTING.md).

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass: `make test`
5. Submit a pull request

## License

Copyright 2025, Seqera Labs

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Support

For issues, questions, or contributions:

- 📖 [Configuration Reference](docs/CONFIG.md)
- 📚 [Example Configurations](example/configs/README.md)
- 🐛 [Report bugs](https://github.com/adamrtalbot/nf-slack/issues)
- 💡 [Request features](https://github.com/adamrtalbot/nf-slack/issues)
