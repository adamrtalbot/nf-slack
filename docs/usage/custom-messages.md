# Custom Messages

Send custom Slack messages from within your workflow scripts using the `slackMessage()` function.

!!! tip "Prerequisites"
The plugin must be enabled and configured for custom messages to work. See [Quick Start](../getting-started/quick-start.md) if you haven't set up the plugin yet.

## Basic Usage

### Simple Text Messages

Send a simple text message:

```groovy
include { slackMessage } from 'plugin/nf-slack'

workflow {
    slackMessage("🔬 Starting analysis for sample ${params.sample_id}")

    // Your workflow logic here
    MY_PROCESS(input_ch)

    slackMessage("✅ Analysis complete!")
}
```

![Simple custom messages](../images/nf-slack-04.png)

## Rich Formatted Messages

### Adding Colors and Fields

Create rich messages with colors and custom fields:

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

### Message Structure

When using the map format, you can specify:

| Property  | Type   | Description                      | Required |
| --------- | ------ | -------------------------------- | -------- |
| `message` | String | Main message text                | Yes      |
| `color`   | String | Hex color code (e.g., "#2EB887") | No       |
| `fields`  | List   | Array of field objects           | No       |

### Field Structure

Each field in the `fields` array can have:

| Property | Type    | Description                            | Required |
| -------- | ------- | -------------------------------------- | -------- |
| `title`  | String  | Field label                            | Yes      |
| `value`  | String  | Field content                          | Yes      |
| `short`  | Boolean | Show in column layout (default: false) | No       |

## Color Reference

Use these colors for consistent message styling:

- **Success**: `#2EB887` (green) - For successful operations
- **Error**: `#A30301` (red) - For errors or failures
- **Info**: `#3AA3E3` (blue) - For informational messages
- **Warning**: `#FFA500` (orange) - For warnings

```groovy
// Success message
slackMessage([
    message: "✅ Pipeline completed successfully",
    color: "#2EB887"
])

// Error message
slackMessage([
    message: "❌ Quality control failed",
    color: "#A30301"
])

// Info message
slackMessage([
    message: "ℹ️ Processing 100 samples",
    color: "#3AA3E3"
])

// Warning message
slackMessage([
    message: "⚠️ Low coverage detected",
    color: "#FFA500"
])
```

## Common Use Cases

### Send Results Summary

Notify when analysis completes with summary statistics:

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

### Progress Updates

Send notifications at key workflow milestones:

```groovy
include { slackMessage } from 'plugin/nf-slack'

workflow {
    slackMessage("🚀 Starting processing of ${sample_count} samples")

    QUALITY_CONTROL(input_ch).map { results ->
        slackMessage("✅ Quality control complete")
    }

    ALIGNMENT(QUALITY_CONTROL.out).map { results ->
        slackMessage("✅ Alignment complete")
    }

    VARIANT_CALLING(ALIGNMENT.out).map { results ->
        slackMessage("✅ Variant calling complete")
    }
}
```

### Conditional Notifications

Send messages based on conditions:

```groovy
include { slackMessage } from 'plugin/nf-slack'

workflow {
    QUALITY_CHECK(input_ch)
        .branch { sample, qc ->
            pass: qc.score >= 30
            fail: qc.score < 30
        }
        .set { qc_results }

    // Notify on failures
    qc_results.fail
        .map { sample, qc ->
            slackMessage([
                message: "⚠️ Quality check failed for ${sample}",
                color: "#FFA500",
                fields: [
                    [title: "Sample", value: sample, short: true],
                    [title: "Score", value: qc.score.toString(), short: true]
                ]
            ])
        }
}
```

### Batch Notifications

Collect results and send a summary:

```groovy
include { slackMessage } from 'plugin/nf-slack'

workflow {
    PROCESS_SAMPLES(input_ch)
        .collect()
        .map { results ->
            def total = results.size()
            def success = results.count { it.status == 'success' }
            def failed = results.count { it.status == 'failed' }

            slackMessage([
                message: "Batch processing complete",
                color: failed > 0 ? "#FFA500" : "#2EB887",
                fields: [
                    [title: "Total Samples", value: total.toString(), short: true],
                    [title: "Successful", value: success.toString(), short: true],
                    [title: "Failed", value: failed.toString(), short: true]
                ]
            ])
        }
}
```

## Best Practices

### Rate Limiting

The plugin includes built-in rate limiting (max 1 message per second) and retry logic. To avoid issues:

- ✅ **Do**: Send messages at key milestones
- ✅ **Do**: Use batch notifications for multiple items
- ✅ **Do**: Use automatic notifications when possible
- ❌ **Don't**: Send messages in tight loops
- ❌ **Don't**: Send a message for every single sample in a large dataset

### Message Design

For better readability:

- Use emojis to convey status at a glance (🚀 ✅ ❌ ⚠️)
- Use the `short: true` flag for fields to display them in columns
- Keep message text concise and informative
- Use colors consistently (green for success, red for errors, etc.)

### Performance

- Slack notifications are sent asynchronously and won't block your workflow
- Failed Slack messages are retried automatically
- Slack failures never cause workflow failures (fail-safe design)

## Troubleshooting

### Messages Not Appearing

1. **Verify plugin is enabled**:

   ```groovy
   slack {
       enabled = true  // Must be true
   }
   ```

2. **Check webhook configuration**:

   ```groovy
   slack {
       webhook {
           url = env.SLACK_WEBHOOK_URL  // Must be set
       }
   }
   ```

3. **Test the webhook** with curl:
   ```bash
   curl -X POST -H 'Content-type: application/json' \
     --data '{"text":"Test message"}' \
     $SLACK_WEBHOOK_URL
   ```

### Rate Limit Errors

If you see rate limit errors in logs:

- Reduce the frequency of messages
- Batch notifications instead of sending individually
- Use `.collect()` to aggregate results before notifying

## Next Steps

- Learn about [automatic notifications](automatic-notifications.md)
- Explore [configuration options](configuration.md)
- View [example gallery](../examples/gallery.md) for real-world patterns
- Check the [API reference](../reference/api.md) for all options
