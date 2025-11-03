# Configuration Reference

Complete API-style reference for all nf-slack plugin configuration options.

## Table of Contents

- [Configuration Reference](#configuration-reference)
  - [Table of Contents](#table-of-contents)
  - [`slack`](#slack)
    - [Example](#example)
  - [`slack.webhook`](#slackwebhook)
    - [Example](#example-1)
  - [`slack.onStart`](#slackonstart)
    - [Properties](#properties)
    - [Message Available Fields](#message-available-fields)
    - [Example](#example-2)
  - [`slack.onComplete`](#slackoncomplete)
    - [Properties](#properties-1)
    - [Message Available Fields](#message-available-fields-1)
    - [Example](#example-3)
  - [`slack.onError`](#slackonerror)
    - [Properties](#properties-2)
    - [Message Available Fields](#message-available-fields-2)
    - [Example](#example-4)
  - [`slack.<scope>.message (String)`](#slackscopemessage-string)
    - [`slack.<scope>.message (Map)`](#slackscopemessage-map)
  - [`slack.<scope>.message.includeFields`](#slackscopemessageincludefields)
  - [Color Reference](#color-reference)

---

## `slack`

| Property     | Type    | Default                       | Required | Description                                                                       |
| ------------ | ------- | ----------------------------- | -------- | --------------------------------------------------------------------------------- |
| `enabled`    | Boolean | `true`                        | No       | Master switch to enable/disable the plugin                                        |
| `webhook`    | Closure | -                             | Yes\*    | Webhook configuration block (see [Webhook Configuration](#webhook-configuration)) |
| `onStart`    | Closure | See [onStart](#onstart)       | No       | Configuration for workflow start notifications                                    |
| `onComplete` | Closure | See [onComplete](#oncomplete) | No       | Configuration for workflow completion notifications                               |
| `onError`    | Closure | See [onError](#onerror)       | No       | Configuration for workflow error notifications                                    |

\*Required only if plugin is enabled. If no webhook is configured, the plugin will automatically disable itself.

### Example

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

## `slack.webhook`

| Property | Type   | Default | Required | Description                                                             |
| -------- | ------ | ------- | -------- | ----------------------------------------------------------------------- |
| `url`    | String | -       | Yes      | Slack Incoming Webhook URL (must start with `https://hooks.slack.com/`) |

### Example

```groovy
webhook {
    url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
}
```

---

## `slack.onStart`

Configuration for workflow start notifications.

### Properties

| Property             | Type          | Default                   | Description                            |
| -------------------- | ------------- | ------------------------- | -------------------------------------- |
| `enabled`            | Boolean       | `true`                    | Send notification when workflow starts |
| `message`            | String or Map | `'🚀 *Pipeline started*'` | Start notification message             |
| `includeCommandLine` | Boolean       | `true`                    | Include command line in message        |

### Message Available Fields

- `runName` - Workflow run name
- `status` - Current status (always "Running" for start messages)
- `commandLine` - Full Nextflow command
- `workDir` - Working directory path

### Example

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

## `slack.onComplete`

Configuration for workflow completion notifications.

### Properties

| Property               | Type          | Default                                  | Description                                |
| ---------------------- | ------------- | ---------------------------------------- | ------------------------------------------ |
| `enabled`              | Boolean       | `true`                                   | Send notification when workflow completes  |
| `message`              | String or Map | `'✅ *Pipeline completed successfully*'` | Completion notification message            |
| `includeCommandLine`   | Boolean       | `true`                                   | Include command line in message            |
| `includeResourceUsage` | Boolean       | `true`                                   | Include task statistics and resource usage |

> **Note**: `includeResourceUsage` is **only available** in the `onComplete` scope.

### Message Available Fields

- `runName` - Workflow run name
- `status` - Final status (e.g., "OK")
- `duration` - Total workflow runtime
- `commandLine` - Full Nextflow command
- `tasks` - Task execution statistics (count, succeeded, failed, cached)

### Example

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

## `slack.onError`

Configuration for workflow error notifications.

### Properties

| Property             | Type          | Default                  | Description                           |
| -------------------- | ------------- | ------------------------ | ------------------------------------- |
| `enabled`            | Boolean       | `true`                   | Send notification when workflow fails |
| `message`            | String or Map | `'❌ *Pipeline failed*'` | Error notification message            |
| `includeCommandLine` | Boolean       | `true`                   | Include command line in message       |

### Message Available Fields

- `runName` - Workflow run name
- `status` - Error status
- `duration` - Time before failure
- `commandLine` - Full Nextflow command
- `errorMessage` - Error details
- `failedProcess` - Name of the process that failed

### Example

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

## `slack.<scope>.message (String)`

Use a string for quick, simple message customization. Supports Slack markdown (`*bold*`, `_italic_`, `` `code` ``), emojis, and newlines (`\n`).

```groovy
onStart {
    message = '🚀 *Pipeline started*\nRunning on cluster'
}
```

---

### `slack.<scope>.message (Map)`

Use a map for full control with colors, fields, and custom data.

**Properties:**

- `text` (required) - Main message text
- `color` - Hex color code (e.g., `#2EB887`)
- `includeFields` - List of default fields (see [Field Reference](#field-reference))
- `customFields` - List of custom fields with `title`, `value`, and optional `short` (boolean for 2-column layout)

**Example:**

```groovy
onComplete {
    message = [
        text: '✅ *Analysis Complete*',
        color: '#2EB887',
        includeFields: ['runName', 'duration', 'status'],
        customFields: [
            [title: 'Sample Count', value: '150', short: true],
            [title: 'Output Location', value: 's3://bucket/results/', short: false]
        ]
    ]
}
```

---

## `slack.<scope>.message.includeFields`

The following fields can be included in the `includeFields` array when using map format:

| Field Name      | Description                                   | Available In            |
| --------------- | --------------------------------------------- | ----------------------- |
| `runName`       | Nextflow run name (e.g., `tiny_euler`)        | All scopes              |
| `status`        | Workflow status with emoji                    | All scopes              |
| `duration`      | Total workflow runtime (e.g., `2h 30m 15s`)   | `onComplete`, `onError` |
| `commandLine`   | Full Nextflow command used to launch workflow | All scopes              |
| `workDir`       | Working directory path                        | `onStart`               |
| `errorMessage`  | Error details and stack trace                 | `onError`               |
| `failedProcess` | Name and tag of the process that failed       | `onError`               |
| `tasks`         | Task execution statistics                     | `onComplete`            |

---

## Color Reference

Standard color codes for Slack message attachments:

| Name           | Hex Code  | Use Case                      |
| -------------- | --------- | ----------------------------- |
| Success Green  | `#2EB887` | Successful completions        |
| Error Red      | `#A30301` | Failures and errors           |
| Info Blue      | `#3AA3E3` | Informational, start messages |
| Warning Orange | `#FFA500` | Warnings                      |
| Neutral Gray   | `#808080` | Neutral information           |

---
