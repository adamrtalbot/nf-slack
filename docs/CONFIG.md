# Configuration Reference

Complete API-style reference for all nf-slack plugin configuration options.

## Table of Contents

- [Root Configuration](#root-configuration)
- [Webhook Configuration](#webhook-configuration)
- [Notification Scopes](#notification-scopes)
  - [onStart](#onstart)
  - [onComplete](#oncomplete)
  - [onError](#onerror)
- [Message Configuration](#message-configuration)
  - [Simple Text Format](#simple-text-format)
  - [Advanced Map Format](#advanced-map-format)
- [Field Reference](#field-reference)
- [Complete Example](#complete-example)

---

## Root Configuration

The `slack` block is the root configuration scope for the plugin.

### `slack { }`

| Property | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `enabled` | Boolean | `true` | No | Master switch to enable/disable the plugin |
| `webhook` | Closure | - | Yes* | Webhook configuration block (see [Webhook Configuration](#webhook-configuration)) |
| `onStart` | Closure | See [onStart](#onstart) | No | Configuration for workflow start notifications |
| `onComplete` | Closure | See [onComplete](#oncomplete) | No | Configuration for workflow completion notifications |
| `onError` | Closure | See [onError](#onerror) | No | Configuration for workflow error notifications |

*Required only if plugin is enabled. If no webhook is configured, the plugin will automatically disable itself.

#### Example

```groovy
slack {
    enabled = true
    webhook { /* ... */ }
    onStart { /* ... */ }
    onComplete { /* ... */ }
    onError { /* ... */ }
}
```

---

## Webhook Configuration

The `webhook` block configures the Slack Incoming Webhook integration.

### `webhook { }`

| Property | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `url` | String | - | Yes | Slack Incoming Webhook URL (must start with `https://hooks.slack.com/`) |

#### Example

```groovy
webhook {
    url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
}
```

#### Using Environment Variables

```groovy
webhook {
    url = System.getenv('SLACK_WEBHOOK_URL')
}
```

#### Using Nextflow Secrets

```groovy
webhook {
    url = secrets.SLACK_WEBHOOK_URL
}
```

---

## Notification Scopes

Each notification type has its own configuration scope with shared and specific properties.

### Common Properties

These properties are available in all notification scopes (`onStart`, `onComplete`, `onError`):

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | `true` | Whether to send this notification type |
| `message` | String or Map | See defaults below | Message text or advanced configuration |
| `includeCommandLine` | Boolean | `true` | Whether to include the Nextflow command line in the message |

---

### `onStart`

Configuration for workflow start notifications.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | `true` | Send notification when workflow starts |
| `message` | String or Map | `'🚀 *Pipeline started*'` | Start notification message |
| `includeCommandLine` | Boolean | `true` | Include command line in message |

#### Available Fields (when using map format)

- `runName` - Workflow run name
- `status` - Current status (always "Running" for start messages)
- `commandLine` - Full Nextflow command
- `workDir` - Working directory path

#### Examples

**Disable start notifications:**
```groovy
onStart {
    enabled = false
}
```

**Custom text message:**
```groovy
onStart {
    message = '🎬 *My pipeline is starting!*'
}
```

**Advanced map format:**
```groovy
onStart {
    message = [
        text: '🚀 *Production Pipeline Starting*',
        color: '#3AA3E3',
        includeFields: ['runName', 'status', 'commandLine'],
        customFields: [
            [title: 'Environment', value: 'Production', short: true],
            [title: 'Priority', value: 'High', short: true]
        ]
    ]
}
```

---

### `onComplete`

Configuration for workflow completion notifications.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | `true` | Send notification when workflow completes |
| `message` | String or Map | `'✅ *Pipeline completed successfully*'` | Completion notification message |
| `includeCommandLine` | Boolean | `true` | Include command line in message |
| `includeResourceUsage` | Boolean | `true` | Include task statistics and resource usage |

> **Note**: `includeResourceUsage` is **only available** in the `onComplete` scope.

#### Available Fields (when using map format)

- `runName` - Workflow run name
- `status` - Final status (e.g., "OK")
- `duration` - Total workflow runtime
- `commandLine` - Full Nextflow command
- `tasks` - Task execution statistics (count, succeeded, failed, cached)

#### Examples

**Custom text message:**
```groovy
onComplete {
    message = '🎉 *Analysis finished!*'
}
```

**Hide resource usage:**
```groovy
onComplete {
    includeResourceUsage = false
}
```

**Advanced map format with selective fields:**
```groovy
onComplete {
    message = [
        text: '✅ *Analysis Complete*',
        color: '#2EB887',
        includeFields: ['runName', 'duration', 'tasks'],
        customFields: [
            [title: 'Results', value: 's3://bucket/results/', short: false],
            [title: 'Cost', value: '$12.50', short: true]
        ]
    ]
    includeResourceUsage = true
}
```

---

### `onError`

Configuration for workflow error notifications.

#### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | `true` | Send notification when workflow fails |
| `message` | String or Map | `'❌ *Pipeline failed*'` | Error notification message |
| `includeCommandLine` | Boolean | `true` | Include command line in message |

#### Available Fields (when using map format)

- `runName` - Workflow run name
- `status` - Error status
- `duration` - Time before failure
- `commandLine` - Full Nextflow command
- `errorMessage` - Error details
- `failedProcess` - Name of the process that failed

#### Examples

**Custom text message:**
```groovy
onError {
    message = '💥 *Pipeline crashed!*'
}
```

**Advanced map format with error details:**
```groovy
onError {
    message = [
        text: '❌ *Pipeline Failed*',
        color: '#A30301',
        includeFields: ['runName', 'duration', 'errorMessage', 'failedProcess'],
        customFields: [
            [title: 'Support', value: 'support@example.com', short: true],
            [title: 'On-Call', value: '@devops', short: true]
        ]
    ]
}
```

---

## Message Configuration

Messages can be configured in two formats: **simple text** or **advanced map**.

### Simple Text Format

A string value for quick, simple message customization.

#### Type

```groovy
String
```

#### Features

- Supports Slack markdown formatting
- Use `*bold*`, `_italic_`, `` `code` ``
- Include emojis with `:emoji_name:` or Unicode
- Newlines with `\n`

#### Example

```groovy
onStart {
    message = '🚀 *Pipeline started*\nRunning on cluster'
}
```

---

### Advanced Map Format

A map configuration for full control over message appearance.

#### Type

```groovy
Map<String, Object>
```

#### Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `text` | String | Yes | Main message text (supports Slack markdown) |
| `color` | String | No | Hex color code for message border (e.g., `#2EB887`) |
| `includeFields` | List<String> | No | List of default fields to include (see [Field Reference](#field-reference)) |
| `customFields` | List<Map> | No | Custom fields to add to the message |

#### Custom Field Structure

Each custom field is a map with:

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `title` | String | Yes | Field label/name |
| `value` | String | Yes | Field content/value |
| `short` | Boolean | No | If `true`, field appears in 2-column layout (default: `false`) |

#### Example

```groovy
onComplete {
    message = [
        text: '✅ *Analysis Complete*',
        color: '#2EB887',  // Green
        includeFields: ['runName', 'duration', 'status'],
        customFields: [
            [title: 'Sample Count', value: '150', short: true],
            [title: 'Output Location', value: 's3://bucket/results/', short: false]
        ]
    ]
}
```

---

## Field Reference

### Available `includeFields` Values

The following fields can be included in the `includeFields` array when using map format:

| Field Name | Description | Available In |
|------------|-------------|--------------|
| `runName` | Nextflow run name (e.g., `tiny_euler`) | All scopes |
| `status` | Workflow status with emoji | All scopes |
| `duration` | Total workflow runtime (e.g., `2h 30m 15s`) | `onComplete`, `onError` |
| `commandLine` | Full Nextflow command used to launch workflow | All scopes |
| `workDir` | Working directory path | `onStart` |
| `errorMessage` | Error details and stack trace | `onError` |
| `failedProcess` | Name and tag of the process that failed | `onError` |
| `tasks` | Task execution statistics | `onComplete` |

### Field Details

#### `runName`
- **Type**: String
- **Example**: `"mad_turing"`
- **Description**: The unique name assigned to this workflow run

#### `status`
- **Type**: String
- **Example**: `"✅ OK"`, `"🚀 Running"`, `"❌ Failed"`
- **Description**: Current workflow status with emoji prefix

#### `duration`
- **Type**: String
- **Example**: `"2h 30m 15s"`
- **Description**: Formatted duration of workflow execution
- **Note**: Not available for `onStart` messages

#### `commandLine`
- **Type**: String
- **Example**: `"nextflow run main.nf -profile docker"`
- **Description**: The complete command used to launch the workflow

#### `workDir`
- **Type**: String
- **Example**: `"/home/user/work"`
- **Description**: Path to the Nextflow working directory
- **Note**: Only available for `onStart` messages

#### `errorMessage`
- **Type**: String
- **Example**: `"Process execution failed"`
- **Description**: Detailed error message and stack trace
- **Note**: Only available for `onError` messages

#### `failedProcess`
- **Type**: String
- **Example**: `"PROCESS_NAME (sample123)"`
- **Description**: Name of the failed process and its tag if available
- **Note**: Only available for `onError` messages

#### `tasks`
- **Type**: Map
- **Example**: `"Total: 150, Succeeded: 148, Failed: 2, Cached: 50"`
- **Description**: Statistics about task execution
- **Note**: Only available for `onComplete` messages

---

## Color Reference

Standard color codes for Slack message attachments:

| Name | Hex Code | Use Case |
|------|----------|----------|
| Success Green | `#2EB887` | Successful completions |
| Error Red | `#A30301` | Failures and errors |
| Info Blue | `#3AA3E3` | Informational, start messages |
| Warning Orange | `#FFA500` | Warnings |
| Neutral Gray | `#808080` | Neutral information |

---

## Complete Example

Comprehensive configuration example demonstrating all features:

```groovy
plugins {
    id 'nf-slack@0.1.0'
}

slack {
    // Master enable switch
    enabled = true

    // Webhook configuration
    webhook {
        url = System.getenv('SLACK_WEBHOOK_URL')
    }

    // Start notification configuration
    onStart {
        enabled = true
        message = [
            text: '🚀 *Production Pipeline Starting*',
            color: '#3AA3E3',
            includeFields: ['runName', 'status', 'commandLine'],
            customFields: [
                [title: 'Environment', value: 'Production', short: true],
                [title: 'Samples', value: params.sample_count, short: true],
                [title: 'Profile', value: workflow.profile, short: true]
            ]
        ]
        includeCommandLine = true
    }

    // Completion notification configuration
    onComplete {
        enabled = true
        message = [
            text: '✅ *Analysis Complete*',
            color: '#2EB887',
            includeFields: ['runName', 'duration', 'status', 'tasks'],
            customFields: [
                [title: 'Results Location', value: "s3://${params.bucket}/results/", short: false],
                [title: 'Report', value: "${params.outdir}/report.html", short: false]
            ]
        ]
        includeCommandLine = false
        includeResourceUsage = true
    }

    // Error notification configuration
    onError {
        enabled = true
        message = [
            text: '❌ *Pipeline Failed*',
            color: '#A30301',
            includeFields: ['runName', 'duration', 'errorMessage', 'failedProcess'],
            customFields: [
                [title: 'Support', value: 'support@example.com', short: true],
                [title: 'On-Call', value: '@devops-team', short: true],
                [title: 'Runbook', value: 'https://wiki.example.com/runbook', short: false]
            ]
        ]
        includeCommandLine = true
    }
}
```

---

## Configuration Best Practices

### Security

1. **Never commit webhook URLs** to version control
2. **Use environment variables** or Nextflow secrets:
   ```groovy
   webhook {
       url = System.getenv('SLACK_WEBHOOK_URL')
   }
   ```
3. **Use different webhooks** for different environments (dev/staging/prod)

### Performance

1. **Disable in development** when not needed:
   ```groovy
   slack {
       enabled = !workflow.profile.contains('dev')
   }
   ```
2. **Be selective with fields** - only include what you need
3. **Minimize custom messages** in tight loops

### Organization

1. **Use profile-specific configs**:
   ```groovy
   profiles {
       prod {
           slack.enabled = true
           slack.webhook.url = System.getenv('SLACK_WEBHOOK_PROD')
       }
       dev {
           slack.enabled = false
       }
   }
   ```

2. **Standardize across pipelines** - use consistent message formats
3. **Document custom fields** - make it clear what they mean

### Readability

1. **Use descriptive custom fields** with clear titles
2. **Keep messages concise** - avoid information overload
3. **Use emojis sparingly** - for visual distinction, not decoration
4. **Format long values** - use `short: false` for long paths or URLs

---

## Validation

The plugin performs the following validations:

1. **Webhook URL**: Must start with `https://hooks.slack.com/`
2. **Color codes**: Must be valid hex colors (e.g., `#RRGGBB`)
3. **includeFields**: Must be valid field names for the notification type
4. **Custom fields**: Must have both `title` and `value`

Invalid configurations will log warnings but will not fail your workflow.

---

## Related Documentation

- [README.md](../README.md) - Main documentation
- [Configuration Examples](../example/configs/README.md) - Progressive examples
- [CONTRIBUTING.md](CONTRIBUTING.md) - Development guide
