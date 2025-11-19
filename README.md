# nf-slack plugin

Get Slack notifications for your Nextflow workflows - automatically notified when pipelines start, complete, or fail.

:book: Full documentation: <https://adamrtalbot.github.io/nf-slack/>

## Features

- 🚀 **Automatic Notifications**: Get notified when workflows start, complete, or fail
- 💬 **Custom Messages**: Send custom messages from within your workflow scripts
- 🎨 **Rich Formatting**: Beautiful Slack messages with colors and custom fields
- ⚙️ **Highly Configurable**: Control what notifications are sent and when
- 🛡️ **Fail-Safe**: Never fails your workflow, even if Slack is unavailable

## Quick Start

### 1. Set up Slack Bot Authentication (Recommended)

**Bot authentication is more secure and capable than webhooks.**

1. Go to [https://api.slack.com/apps](https://api.slack.com/apps) and create a new app
2. Add bot permissions: `chat:write` (and optionally `chat:write.public`)
3. Install the app to your workspace and copy the **Bot User OAuth Token** (starts with `xoxb-`)
4. Get your channel ID from Slack (click channel name → scroll down to see ID)

See the [Bot Setup Guide](./docs/getting-started/bot-setup.md) for detailed instructions.

### 2. Add to Your Pipeline

Add to your `nextflow.config`:

```groovy
plugins {
    id 'nf-slack@0.2.1'
}

slack {
    bot {
        token = 'xoxb-your-bot-token-here'  // Or use environment variables
        channel = 'C1234567890'              // Your channel ID
    }
}
```

<details>
<summary><b>Alternative: Webhook authentication (legacy)</b></summary>

```groovy
slack {
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }
}
```

Note: If both bot and webhook are configured, bot takes precedence.

</details>

Will post these messages to Slack:

![default Slack message](./docs/images/nf-slack-00.png)

### 3. Run Your Pipeline

That's it! You'll automatically receive notifications when your pipeline starts, completes, or fails.

## What You Get

Once configured, you'll automatically receive Slack messages for:

- 🚀 **Pipeline starts** - Know when your workflow begins
- ✅ **Successful completions** - Celebrate when pipelines finish
- ❌ **Failures** - Get alerted immediately when something goes wrong

Each message includes relevant details like run name, duration, and error information.

## Basic Customization

### Choose Which Events to Notify

By default, all notifications are enabled. You can selectively disable them:

```groovy
slack {
    bot {
        token = 'xoxb-your-bot-token-here'
        channel = 'C1234567890'
    }

    onStart.enabled = false     // Do not notify when pipeline starts
    onComplete.enabled = true   // Notify on successful completion
    onError.enabled = true      // Notify on failures
}
```

Will post this message to Slack:

![Slack message on completion or error](./docs/images/nf-slack-01.png)

### Customize Message Text

Change the notification messages to suit your needs:

```groovy
slack {
    bot {
        token = 'xoxb-your-bot-token-here'
        channel = 'C1234567890'
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

Will post these messages to Slack, note the text has changed:

![Custom Slack messages](./docs/images/nf-slack-02.png)

### Add Colors and Custom Fields

For richer messages with colors and additional information:

```groovy
slack {
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }

    onComplete {
        message = [
            text: '✅ *Analysis Complete*',
            color: '#2EB887',  // Green color
            customFields: [
                [title: 'Environment', value: 'Production', short: true],
                [title: 'Cost', value: '$12.50', short: true]
            ]
        ]
    }
}
```

Will post these formatted messages to Slack:

![Custom Slack blocks](./docs/images/nf-slack-03.png)

### Send Custom Messages from Your Workflow

Send notifications from within your pipeline code:

```groovy
include { slackMessage } from 'plugin/nf-slack'

params.sample_id = "SAMPLE_001"

workflow {
    slackMessage("🔬 Starting analysis for sample ${params.sample_id}")

    // Your workflow processes here

    slackMessage("✅ Analysis complete!")
}
```

Will post these short messages to Slack within the workflow:

![Workflow Slack message](./docs/images/nf-slack-04.png)

**Want more control?** See the [API Reference](docs/REFERENCE.md) and [detailed examples](docs/EXAMPLES.md) for all available options.

## Examples

We provide 6 progressive configuration examples from basic to advanced:

| Example                                                                          | Description                               |
| -------------------------------------------------------------------------------- | ----------------------------------------- |
| [01-minimal.config](example/configs/01-minimal.config)                           | Just webhook, use defaults                |
| [02-notification-control.config](example/configs/02-notification-control.config) | Choose which events trigger notifications |
| [03-message-text.config](example/configs/03-message-text.config)                 | Change notification text                  |
| [04-message-colors.config](example/configs/04-message-colors.config)             | Customize colors                          |
| [05-custom-fields.config](example/configs/05-custom-fields.config)               | Add your own information                  |
| [06-selective-fields.config](example/configs/06-selective-fields.config)         | Select which workflow info to show        |

**[View all examples with explanations →](docs/EXAMPLES.md)**

## Documentation

- **[API Reference](docs/REFERENCE.md)** - Complete configuration options and API reference
- **[Usage Guide](docs/USAGE.md)** - How to use the plugin and send custom messages
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions
- **[Examples](docs/EXAMPLES.md)** - Progressive configuration examples
- **[Contributing](docs/CONTRIBUTING.md)** - Development setup and contribution guidelines

## Support

- 🐛 [Report bugs](https://github.com/adamrtalbot/nf-slack/issues)
- 💡 [Request features](https://github.com/adamrtalbot/nf-slack/issues)
- 📖 [Read the docs](docs/)

## License

Copyright 2025, Seqera Labs. Licensed under the Apache License, Version 2.0.
