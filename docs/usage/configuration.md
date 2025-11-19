# Configuration

Advanced configuration options for customizing nf-slack notifications.

## Overview

The nf-slack plugin is configured through the `slack` block in your `nextflow.config`. All configuration options are optional except for the webhook URL.

## Basic Structure

```groovy
slack {
    enabled = true
    bot {
        token = "$SLACK_BOT_TOKEN"
        channel = 'C123456'
    }
    onStart { /* ... */ }
    onComplete { /* ... */ }
    onError { /* ... */ }
}
```

## Master Switch

### Enable/Disable the Plugin

```groovy
slack {
    enabled = true  // Set to false to disable all Slack functionality
}
```

When `enabled = false`:

- No automatic notifications are sent
- Custom `slackMessage()` calls are silently ignored
- No Slack API calls are made

## Bot Configuration

### Basic Bot Setup

```groovy
slack {
    bot {
        token = 'xoxb-your-bot-token'
        channel = 'C123456'
    }
}
```

### Using Environment Variables

```groovy
slack {
    bot {
        token = "$SLACK_BOT_TOKEN"
        channel = 'C123456'
    }
}
```

### Using Nextflow Secrets

```groovy
slack {
    bot {
        token = secrets.SLACK_BOT_TOKEN
        channel = 'C123456'
    }
}
```

!!! tip "Security Best Practice"

    Never hardcode tokens in configuration files that are committed to version control. Use environment variables or Nextflow secrets.

## Event Notification Control

### Enable/Disable Individual Events

Control which workflow events trigger notifications:

```groovy
slack {
    onStart {
        enabled = true  // Notify when workflow starts
    }

    onComplete {
        enabled = true  // Notify when workflow completes
    }

    onError {
        enabled = true  // Notify when workflow fails
    }
}
```

## Customizing Messages

### Simple Text Messages

Replace the default message text:

```groovy
slack {
    onStart {
        message = '🎬 *Production pipeline is starting!*'
    }

    onComplete {
        message = '🎉 *Analysis completed successfully!*'
    }

    onError {
        message = '💥 *Pipeline encountered an error!*'
    }
}
```

![Custom message text](../images/nf-slack-02.png)

### Adding Custom Fields

Include additional information in notifications:

```groovy
slack {
    onComplete {
        message = [
            text: '✅ *Analysis Complete*',
            customFields: [
                [title: 'Environment', value: 'Production', short: true],
                [title: 'Cost', value: '$12.50', short: true]
            ]
        ]
    }
}
```

![Custom fields](../images/nf-slack-03.png)

### Field Options

Each field in `customFields` can have:

| Property | Type    | Description                         | Required |
| -------- | ------- | ----------------------------------- | -------- |
| `title`  | String  | Field label                         | Yes      |
| `value`  | String  | Field content                       | Yes      |
| `short`  | Boolean | Display in columns (default: false) | No       |

## Controlling Workflow Information

### Include/Exclude Workflow Fields

Choose which workflow information to display:

```groovy
slack {
    onComplete {
        message = [
            text: '✅ *Pipeline Complete*',
            includeFields: ['runName', 'duration', 'tasks']
            // Only shows: run name, duration, and task statistics
        ]
    }
}
```

### Available Workflow Fields

**onStart fields:**

- `runName` - Workflow run name
- `status` - Current status
- `commandLine` - Full Nextflow command
- `workDir` - Working directory path

**onComplete fields:**

- `runName` - Workflow run name
- `status` - Final status
- `duration` - Total workflow runtime
- `commandLine` - Full Nextflow command
- `tasks` - Task execution statistics

**onError fields:**

- `runName` - Workflow run name
- `status` - Error status
- `duration` - Time before failure
- `commandLine` - Full Nextflow command
- `errorMessage` - Error details
- `failedProcess` - Name of failed process

### Command Line Control

Control whether the command line is included:

```groovy
slack {
    onStart {
        includeCommandLine = false  // Don't show command line
    }

    onComplete {
        includeCommandLine = true  // Show command line (default)
    }
}
```

### Resource Usage (onComplete only)

Control whether task statistics and resource usage are included:

```groovy
slack {
    onComplete {
        includeResourceUsage = true  // Show task stats (default)
    }
}
```

## Complete Configuration Example

```groovy
plugins {
    id 'nf-slack@0.2.1'
}

slack {
    enabled = true

    bot {
        token = "$SLACK_BOT_TOKEN"
        channel = 'C123456'
    }

    onStart {
        enabled = true
        message = [
            text: '🚀 *Production Pipeline Starting*',
            customFields: [
                [title: 'Environment', value: 'Production', short: true],
                [title: 'Priority', value: 'High', short: true]
            ]
        ]
        includeCommandLine = true
    }

    onComplete {
        enabled = true
        message = [
            text: '✅ *Pipeline Completed*',
            includeFields: ['runName', 'duration', 'tasks'],
            customFields: [
                [title: 'Cost', value: '$12.50', short: true]
            ]
        ]
        includeResourceUsage = true
    }

    onError {
        enabled = true
        message = [
            text: '❌ *Pipeline Failed*',
            includeFields: ['runName', 'errorMessage', 'failedProcess']
        ]
        includeCommandLine = true
    }
}
```

## Next Steps

- Learn about [automatic notifications](automatic-notifications.md)
- Explore [custom messages](custom-messages.md) from workflows
- View [example gallery](../examples/gallery.md)
- Check the complete [API Reference](../reference/api.md)
