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

    // Notification settings (all optional - defaults shown)
    onStart {
        enabled = true
    }

    onComplete {
        enabled = true
    }

    onError {
        enabled = true
    }
}
```

### 3. Run Your Pipeline

That's it! You'll now receive Slack notifications for your workflow events.

## 📚 Configuration Examples

We provide **6 progressive examples** demonstrating plugin features from basic to advanced:

| Example | Feature | Description |
|---------|---------|-------------|
| [01-minimal.config](example/configs/01-minimal.config) | Enable notifications | Just webhook, use defaults |
| [02-notification-control.config](example/configs/02-notification-control.config) | Control when to notify | Choose which events trigger notifications |
| [03-message-text.config](example/configs/03-message-text.config) | Customize message text | Change notification text with strings |
| [04-message-colors.config](example/configs/04-message-colors.config) | Customize colors | Use custom colors (map format) |
| [05-custom-fields.config](example/configs/05-custom-fields.config) | Add custom fields | Include your own information |
| [06-selective-fields.config](example/configs/06-selective-fields.config) | Choose default fields | Select which workflow info to show |

Each example focuses on **one specific feature** and builds upon the previous ones.

**[View all examples with detailed documentation →](example/configs/README.md)**

## Configuration

### Basic Configuration

```groovy
slack {
    // Required: Webhook configuration
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }

    // Configure start notifications
    onStart {
        enabled = true                  // Send notification when workflow starts
        message = '🚀 *Pipeline started*'  // Custom message (optional)
        includeCommandLine = true       // Include command line in message
    }

    // Configure completion notifications
    onComplete {
        enabled = true                  // Send notification when workflow completes
        message = '✅ *Pipeline completed*'  // Custom message (optional)
        includeCommandLine = true       // Include command line in message
        includeResourceUsage = true     // Include task statistics
    }

    // Configure error notifications
    onError {
        enabled = true                  // Send notification when workflow fails
        message = '❌ *Pipeline failed*'  // Custom message (optional)
        includeCommandLine = true       // Include command line in message
    }
}
```

### Customizing Default Messages

You can customize the default notification messages in two ways:

#### Simple Text Customization

```groovy
slack {
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }

    // Simple string templates (supports Slack markdown formatting)
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

#### Advanced Message Customization

For full control over message design, colors, and fields, use map-based configuration:

```groovy
slack {
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }

    // Customize start message with specific fields
    onStart {
        message = [
            text: '🚀 *Production Pipeline Starting*',
            color: '#3AA3E3',  // Custom blue color
            includeFields: ['runName', 'status', 'commandLine'],  // Choose which default fields to include
            customFields: [
                [title: 'Environment', value: 'Production', short: true],
                [title: 'Priority', value: 'High', short: true]
            ]
        ]
    }

    // Customize completion message
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

    // Customize error message
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

**Available includeFields options:**
- `runName` - The Nextflow run name
- `status` - Workflow status with emoji
- `duration` - How long the workflow ran (not available for start messages)
- `commandLine` - The command used to launch the workflow
- `workDir` - The work directory (only for start messages)
- `errorMessage` - Error details (only for error messages)
- `failedProcess` - Which process failed (only for error messages)
- `tasks` - Task statistics (only for completion messages)

**Field structure for customFields:**
- `title` (required) - Field label
- `value` (required) - Field content
- `short` (optional) - If `true`, field appears in column layout (default: `false`)

Default messages (if not customized):
- **Start**: `🚀 *Pipeline started*`
- **Complete**: `✅ *Pipeline completed successfully*`
- **Error**: `❌ *Pipeline failed*`

### Disabling the Plugin

```groovy
slack {
    enabled = false
}
```

Or simply don't configure a webhook - the plugin will disable itself if no webhook URL is configured.

## Custom Messages from Workflows

You can send custom messages from within your workflow scripts:

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

### Color Codes

- **Success**: `#2EB887` (green)
- **Error**: `#A30301` (red)
- **Info**: `#3AA3E3` (blue)

### Message Fields

Each field in the `fields` array can have:
- `title` (required): Field label
- `value` (required): Field content
- `short` (optional): If `true`, field appears in a column layout (default: `false`)

## Example Use Cases

### Notify on Long-Running Process

```groovy
process LONG_ANALYSIS {
    input:
    path input_file

    script:
    """
    # Start notification
    slackMessage("⏱️ Starting long analysis on ${input_file}")

    # Run your analysis
    run_analysis.py ${input_file}

    # Completion notification
    slackMessage("✅ Analysis complete for ${input_file}")
    """
}
```

### Send Results Summary

```groovy
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

## Building & Development

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

## Publishing

Plugins can be published to the Nextflow Plugin Registry to make them accessible to the community.

### Prerequisites

1. Create `$HOME/.gradle/gradle.properties` with:
   ```properties
   npr.apiKey=YOUR_NEXTFLOW_PLUGIN_REGISTRY_TOKEN
   ```

2. Create a release:
   ```bash
   make release
   ```

> [!NOTE]
> The Nextflow Plugin Registry is currently available as preview technology. Contact info@nextflow.io to learn how to get access.

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
- 🐛 [Report bugs](https://github.com/yourusername/nf-slack/issues)
- 💡 [Request features](https://github.com/yourusername/nf-slack/issues)
- 📖 [View documentation](https://github.com/yourusername/nf-slack/wiki)
