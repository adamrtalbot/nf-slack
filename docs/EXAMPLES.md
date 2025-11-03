# nf-slack Examples

This directory contains comprehensive examples demonstrating the nf-slack plugin features, from basic to advanced.

The examples are organized into two categories:

- **Configuration Examples** (`configs/`): 6 examples showing automatic workflow notifications
- **Script Examples** (`scripts/`): 3 examples showing programmatic message sending

Each example focuses on **one specific aspect** of the plugin, building upon the previous examples.

## 📋 Quick Reference

### Configuration Examples (Automatic Notifications)

| Example                                                           | Feature                             | Lines |
| ----------------------------------------------------------------- | ----------------------------------- | ----- |
| [01-minimal.config](#example-1-minimal-setup)                     | Enable notifications                | ~25   |
| [02-notification-control.config](#example-2-notification-control) | Control when to notify              | ~30   |
| [03-message-text.config](#example-3-message-text-customization)   | Customize message text              | ~35   |
| [04-message-colors.config](#example-4-message-colors)             | Customize message colors            | ~45   |
| [05-custom-fields.config](#example-5-custom-fields)               | Add custom fields                   | ~55   |
| [06-selective-fields.config](#example-6-selective-default-fields) | Choose which default fields to show | ~70   |

### Script Examples (Programmatic Messages)

| Example                                                                  | Feature                              | Lines |
| ------------------------------------------------------------------------ | ------------------------------------ | ----- |
| [01-message-in-workflow.nf](#script-example-1-message-in-workflow)       | Send message from workflow body      | ~55   |
| [02-message-on-complete.nf](#script-example-2-message-on-complete)       | Send message using onComplete hook   | ~48   |
| [03-message-within-channel.nf](#script-example-3-message-within-channel) | Send messages from channel operators | ~34   |

## 🚀 Getting Started

### Prerequisites

Set up your Slack webhook:

```bash
export SLACK_WEBHOOK_URL='https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
```

### Running Examples

**Configuration Examples**:

```bash
# Run any workflow with a config example
nextflow run main.nf -c configs/01-minimal.config
```

## 📚 Configuration Examples Explained

Configuration examples show how to set up automatic workflow notifications using the plugin's configuration options.

### Example 1: Minimal Setup

**File**: `01-minimal.config`
**Concept**: Just enable notifications with defaults

```groovy
slack {
    webhook {
        url = System.getenv('SLACK_WEBHOOK_URL')
    }
}
```

**What you get**:

- Notifications on start, complete, and error
- Default message templates
- Default bot name: "Nextflow Bot"
- Default icon: :rocket:

**Use when**: You just want to enable notifications quickly

**Example output**:

![Minimal Setup Example](imgs/nf-slack-examples-01.png)

---

### Example 2: Notification Control

**File**: `02-notification-control.config`
**Concept**: Choose which events trigger notifications

**New configuration options**:

- `onStart.enabled` - Send notification when workflow starts
- `onComplete.enabled` - Send notification when workflow completes
- `onError.enabled` - Send notification when workflow fails

```groovy
slack {
    webhook {
        url = System.getenv('SLACK_WEBHOOK_URL')
    }

    onStart {
        enabled = false      // Don't notify on start
    }

    onComplete {
        enabled = true       // DO notify on completion
    }

    onError {
        enabled = true       // DO notify on error
    }
}
```

**Use when**: You want to reduce notification noise (e.g., only errors and completions)

**Example output**:

![Notification Control Example](imgs/nf-slack-examples-02.png)

---

### Example 3: Message Text Customization

**File**: `03-message-text.config`
**Concept**: Customize the text in notification messages

**New configuration options**:

- `onStart.message` - Custom text for start notifications
- `onComplete.message` - Custom text for completion notifications
- `onError.message` - Custom text for error notifications

```groovy
slack {
    webhook {
        url = System.getenv('SLACK_WEBHOOK_URL')
    }

    onStart {
        message = '🚀 *My workflow is starting...*'
    }

    onComplete {
        message = '✅ *My workflow finished successfully!*'
    }

    onError {
        message = '❌ *My workflow failed!*'
    }
}
```

**Supports**: Slack markdown formatting (`*bold*`, `_italic_`, `` `code` ``)

**Use when**: You want different message text than the defaults

**Example output**:

![Message Text Customization Example](imgs/nf-slack-examples-03.png)

---

### Example 4: Message Colors

**File**: `04-message-colors.config`
**Concept**: Use custom colors for message attachments

**New message format**: Map-based configuration instead of strings

```groovy
slack {
    webhook {
        url = System.getenv('SLACK_WEBHOOK_URL')
    }

    onStart {
        message = [
            text: '🚀 *Pipeline started*',
            color: '#3AA3E3'  // Blue
        ]
    }

    onComplete {
        message = [
            text: '✅ *Pipeline completed*',
            color: '#2EB887'  // Green
        ]
    }

    onError {
        message = [
            text: '❌ *Pipeline failed*',
            color: '#A30301'  // Red
        ]
    }
}
```

**Map structure**:

- `text` - Message text (same as string format)
- `color` - Hex color code (e.g., `'#FF5733'`)

**Use when**: You want visual distinction with custom colors

**Example output**:

![Message Colors Example](imgs/nf-slack-examples-04.png)

---

### Example 5: Custom Fields

**File**: `05-custom-fields.config`
**Concept**: Add your own custom information fields

**New map option**: `customFields` array

```groovy
slack {
    webhook {
        url = System.getenv('SLACK_WEBHOOK_URL')
    }

    onStart {
        message = [
            text: '🚀 *Pipeline started*',
            color: '#3AA3E3',
            customFields: [
                [title: 'Priority', value: 'High', short: true],
                [title: 'Team', value: 'Bioinformatics', short: true],
                [title: 'Notes', value: 'Running with increased resources', short: false]
            ]
        ]
    }
}
```

**Field structure**:

- `title` - Field label (required)
- `value` - Field content (required)
- `short` - Layout: `true` = columns (2 per row), `false` = full width

**Use when**: You want to add extra context to messages

**Example output**:

![Custom Fields Example](imgs/nf-slack-examples-05.png)

---

### Example 6: Selective Default Fields

**File**: `06-selective-fields.config`
**Concept**: Choose which built-in workflow information to include

**New map option**: `includeFields` array

```groovy
slack {
    webhook {
        url = System.getenv('SLACK_WEBHOOK_URL')
    }

    onStart {
        message = [
            text: '🚀 *Pipeline started*',
            color: '#3AA3E3',
            includeFields: ['runName', 'status']
        ]
    }

    onComplete {
        message = [
            text: '✅ *Pipeline completed*',
            color: '#2EB887',
            includeFields: ['runName', 'duration', 'status', 'tasks']
        ]
    }

    onError {
        message = [
            text: '❌ *Pipeline failed*',
            color: '#A30301',
            includeFields: ['runName', 'duration', 'errorMessage', 'failedProcess']
        ]
    }
}
```

**Available fields**:

**All messages**:

- `runName` - Nextflow run name
- `status` - Workflow status with emoji

**Start messages only**:

- `commandLine` - Command used to launch workflow
- `workDir` - Work directory path

**Complete messages only**:

- `duration` - How long the workflow ran
- `tasks` - Task statistics (cached, completed, failed)

**Error messages only**:

- `duration` - How long before failure
- `errorMessage` - Error details
- `failedProcess` - Which process failed

**Important**: If you use map-based config without `includeFields`, NO default fields are included (only your `customFields` if specified).

**Use when**: You want fine-grained control over what information appears

**See also**: [API Reference](REFERENCE.md) for complete field documentation and all available options.

**Example output**:

![Selective Fields Example](imgs/nf-slack-examples-06.png)

---

## 📝 Script Examples Explained

Script examples demonstrate how to use the `slackMessage` function programmatically within your Nextflow workflows. These examples show you can send custom messages at any point during workflow execution.

> [!NOTE]
> We have disabled the automatic notifications in the main `nextflow.config` to avoid duplicate messages when running these script examples. Make sure to adjust your configuration accordingly:
>
> ```groovy
> slack {
>     // Rest of content...
>
>     onStart.enabled = false
>     onComplete.enabled = false
>     onError.enabled = false
> }
> ```

**Script Examples** (programmatic messages):

```bash
# Run a script example directly
nextflow run scripts/01-message-in-workflow.nf -c nextflow.config
```

### Script Example 1: Message in Workflow

**File**: `scripts/01-message-in-workflow.nf`
**Concept**: Send a message from the workflow body after processes complete

```groovy
#!/usr/bin/env nextflow

/*
 * Simple example demonstrating nf-slack plugin
 *
 * This workflow sends Slack messages:
 * - AFTER: At workflow completion
 */

// Import the Slack messaging function
include { slackMessage } from 'plugin/nf-slack'

process HELLO {
    input:
    val sample_id

    output:
    stdout

    script:
    """
    echo "Processing sample: ${sample_id}"
    sleep 2  # Simulate some work
    echo "${sample_id}_processed"
    """
}

workflow {
    inputs = channel.of('sample_1', 'sample_2', 'sample_3')
    HELLO(inputs)

    // ==============================================================
    // AFTER: Send rich formatted completion message in workflow body
    // ==============================================================
    slackMessage([
        message: "Example workflow complete! 🎉",
        color: "#2EB887",  // Green for success
        fields: [
            [
                title: "Status",
                value: "Success",
                short: true
            ],
            [
                title: "Samples",
                value: "3",
                short: true
            ]
        ]
    ])
}
```

**Key features**:

- Uses `slackMessage()` function directly in workflow body
- Sends message after processes complete
- Supports same map format as config examples (text, color, fields)

**Use when**: You want to send a message at a specific point in your workflow logic

**Example output**:

![Message in Workflow Example](imgs/nf-slack-examples-07.png)

---

### Script Example 2: Message on Complete

**File**: `scripts/02-message-on-complete.nf`
**Concept**: Send a message using the `workflow.onComplete` event handler

```groovy
#!/usr/bin/env nextflow

include { slackMessage } from 'plugin/nf-slack'

process HELLO {
    input:
    val sample_id

    output:
    stdout

    script:
    """
    echo "Processing sample: ${sample_id}"
    """
}

workflow {
    inputs = channel.of('sample_1', 'sample_2', 'sample_3')
    HELLO(inputs)

    // Send message when workflow completes
    workflow.onComplete = {
        def status = workflow.success ? '✅ SUCCESS' : '❌ FAILED'
        def color = workflow.success ? '#2EB887' : '#A30301'

        slackMessage([
            message: "Workflow ${status}",
            color: color,
            fields: [
                [
                    title: "Duration",
                    value: "${workflow.duration}",
                    short: true
                ]
            ]
        ])
    }
}
```

**Key features**:

- Uses `workflow.onComplete` event handler
- Access to workflow metadata (success status, duration, etc.)
- Conditional message formatting based on success/failure

**Use when**: You want to send a summary message when the workflow finishes, with access to workflow metadata

**Example output**:

![Message on Complete Example](imgs/nf-slack-examples-08.png)

---

### Script Example 3: Message within Channel

**File**: `scripts/03-message-within-channel.nf`
**Concept**: Send messages from within channel operators during processing

```groovy
#!/usr/bin/env nextflow

include { slackMessage } from 'plugin/nf-slack'

process HELLO {
    input:
    val sample_id

    output:
    stdout

    script:
    """
    echo "Processing sample: ${sample_id}"
    """
}

workflow {
    inputs = channel.of('sample_1', 'sample_2', 'sample_3')
        .map { sample ->
            // Send a message for each item
            slackMessage("⚙️ Processing ${sample}")
            return sample
        }

    HELLO(inputs)
}
```

**Key features**:

- Uses `slackMessage()` within channel operator (`.map`)
- Sends individual messages for each channel item
- Simple string format for quick notifications

**Use when**: You want to send notifications during data processing, such as tracking progress through a channel

**Example output**:

![Message within Channel Example](imgs/nf-slack-examples-09.png)
