# nf-slack Configuration Examples

This directory contains 6 progressive examples demonstrating the nf-slack plugin features, from basic to advanced.

Each example focuses on **one specific aspect** of the plugin, building upon the previous examples.

## 📋 Quick Reference

| Example | Feature | Lines |
|---------|---------|-------|
| [01-minimal.config](#example-1-minimal-setup) | Enable notifications | ~25 |
| [02-notification-control.config](#example-2-notification-control) | Control when to notify | ~30 |
| [03-message-text.config](#example-3-message-text-customization) | Customize message text | ~35 |
| [04-message-colors.config](#example-4-message-colors) | Customize message colors | ~45 |
| [05-custom-fields.config](#example-5-custom-fields) | Add custom fields | ~55 |
| [06-selective-fields.config](#example-6-selective-default-fields) | Choose which default fields to show | ~70 |

## 🚀 Getting Started

### Prerequisites

Set up your Slack webhook:
```bash
export SLACK_WEBHOOK_URL='https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
```

### Running an Example

```bash
nextflow run your_pipeline.nf -c example/configs/01-minimal.config
```

## 📚 Examples Explained

### Example 1: Minimal Setup

**File**: `01-minimal.config`
**Concept**: Just enable notifications with defaults

```groovy
slack {
    webhook = System.getenv('SLACK_WEBHOOK_URL')
}
```

**What you get**:
- Notifications on start, complete, and error
- Default message templates
- Default bot name: "Nextflow Bot"
- Default icon: :rocket:

**Use when**: You just want to enable notifications quickly

---

### Example 2: Notification Control

**File**: `02-notification-control.config`
**Concept**: Choose which events trigger notifications

**New configuration options**:
- `notifyOnStart` - Send notification when workflow starts
- `notifyOnComplete` - Send notification when workflow completes
- `notifyOnError` - Send notification when workflow fails

```groovy
slack {
    webhook = System.getenv('SLACK_WEBHOOK_URL')

    notifyOnStart = false      // Don't notify on start
    notifyOnComplete = true    // DO notify on completion
    notifyOnError = true       // DO notify on error
}
```

**Use when**: You want to reduce notification noise (e.g., only errors and completions)

---

### Example 3: Message Text Customization

**File**: `03-message-text.config`
**Concept**: Customize the text in notification messages

**New configuration options**:
- `startMessage` - Custom text for start notifications
- `completeMessage` - Custom text for completion notifications
- `errorMessage` - Custom text for error notifications

```groovy
slack {
    webhook = System.getenv('SLACK_WEBHOOK_URL')

    startMessage = '🚀 *My workflow is starting...*'
    completeMessage = '✅ *My workflow finished successfully!*'
    errorMessage = '❌ *My workflow failed!*'
}
```

**Supports**: Slack markdown formatting (`*bold*`, `_italic_`, `` `code` ``)

**Use when**: You want different message text than the defaults

---

### Example 4: Message Colors

**File**: `04-message-colors.config`
**Concept**: Use custom colors for message attachments

**New message format**: Map-based configuration instead of strings

```groovy
slack {
    webhook = System.getenv('SLACK_WEBHOOK_URL')

    startMessage = [
        text: '🚀 *Pipeline started*',
        color: '#3AA3E3'  // Blue
    ]

    completeMessage = [
        text: '✅ *Pipeline completed*',
        color: '#2EB887'  // Green
    ]

    errorMessage = [
        text: '❌ *Pipeline failed*',
        color: '#A30301'  // Red
    ]
}
```

**Map structure**:
- `text` - Message text (same as string format)
- `color` - Hex color code (e.g., `'#FF5733'`)

**Use when**: You want visual distinction with custom colors

---

### Example 5: Custom Fields

**File**: `05-custom-fields.config`
**Concept**: Add your own custom information fields

**New map option**: `customFields` array

```groovy
slack {
    webhook = System.getenv('SLACK_WEBHOOK_URL')

    startMessage = [
        text: '🚀 *Pipeline started*',
        color: '#3AA3E3',
        customFields: [
            [title: 'Priority', value: 'High', short: true],
            [title: 'Team', value: 'Bioinformatics', short: true],
            [title: 'Notes', value: 'Running with increased resources', short: false]
        ]
    ]
}
```

**Field structure**:
- `title` - Field label (required)
- `value` - Field content (required)
- `short` - Layout: `true` = columns (2 per row), `false` = full width

**Use when**: You want to add extra context to messages

---

### Example 6: Selective Default Fields

**File**: `06-selective-fields.config`
**Concept**: Choose which built-in workflow information to include

**New map option**: `includeFields` array

```groovy
slack {
    webhook = System.getenv('SLACK_WEBHOOK_URL')

    startMessage = [
        text: '🚀 *Pipeline started*',
        color: '#3AA3E3',
        includeFields: ['runName', 'status']
    ]

    completeMessage = [
        text: '✅ *Pipeline completed*',
        color: '#2EB887',
        includeFields: ['runName', 'duration', 'status', 'tasks']
    ]

    errorMessage = [
        text: '❌ *Pipeline failed*',
        color: '#A30301',
        includeFields: ['runName', 'duration', 'errorMessage', 'failedProcess']
    ]
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

---

## 🎨 Feature Progression

```
01: webhook only
    ↓
02: + notification control (on/off switches)
    ↓
03: + message text (simple strings)
    ↓
04: + message colors (map format introduced)
    ↓
05: + custom fields (add your own info)
    ↓
06: + selective fields (choose default fields)
```

## 🔗 Combining Features

You can combine features from different examples:

```groovy
slack {
    webhook = System.getenv('SLACK_WEBHOOK_URL')

    // From example 2: notification control
    notifyOnStart = false
    notifyOnComplete = true
    notifyOnError = true

    // From examples 4, 5, 6: advanced message config
    completeMessage = [
        text: '✅ *Pipeline completed*',
        color: '#2EB887',
        includeFields: ['runName', 'duration', 'tasks'],
        customFields: [
            [title: 'Quality', value: 'Passed', short: true]
        ]
    ]
}
```

## 📖 Configuration Reference

### Basic Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `webhook` | String | (required) | Slack webhook URL |
| `enabled` | Boolean | `true` | Enable/disable plugin |
| `notifyOnStart` | Boolean | `true` | Send start notifications |
| `notifyOnComplete` | Boolean | `true` | Send completion notifications |
| `notifyOnError` | Boolean | `true` | Send error notifications |
| `username` | String | `'Nextflow Bot'` | Bot display name |
| `iconEmoji` | String | `':rocket:'` | Bot icon emoji |
| `includeCommandLine` | Boolean | `true` | Include command in messages |
| `includeResourceUsage` | Boolean | `true` | Include resource stats |

### Message Configuration

**String format** (simple):
```groovy
startMessage = '🚀 *My message*'
```

**Map format** (advanced):
```groovy
startMessage = [
    text: '🚀 *My message*',              // Message text
    color: '#3AA3E3',                     // Hex color
    includeFields: ['runName', 'status'], // Default fields to include
    customFields: [                       // Your custom fields
        [title: 'Field', value: 'Value', short: true]
    ]
]
```

## 🆘 Troubleshooting

### Messages not appearing

1. Verify webhook URL is correct
2. Check plugin is loaded: `nextflow plugin list`
3. Test webhook directly:
   ```bash
   curl -X POST -H 'Content-type: application/json' \
     --data '{"text":"Test"}' \
     $SLACK_WEBHOOK_URL
   ```

### Icon emoji not showing

- Must be a valid Slack emoji (`:rocket:`, `:dna:`, etc.)
- Custom emojis must be created in your Slack workspace first

### Custom fields not appearing

- Verify map structure is correct
- Ensure `customFields` is an array of maps
- Each field needs `title` and `value`

## 📖 Additional Resources

- [Main README](../../README.md) - Full plugin documentation
- [Slack Emoji Cheat Sheet](https://www.webfx.com/tools/emoji-cheat-sheet/) - Valid emoji names
- [Slack API Docs](https://api.slack.com/messaging/webhooks) - Webhook details

## License

Apache 2.0 - Same as the nf-slack plugin
