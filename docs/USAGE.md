# Usage Guide

This guide covers how to use nf-slack in your Nextflow workflows.

## Automatic Notifications

Once configured, nf-slack automatically sends notifications for workflow events. No code changes needed!

**Default behavior:**

- 🚀 Notification when workflow starts
- ✅ Notification when workflow completes successfully
- ❌ Notification when workflow fails

See [Configuration Reference](CONFIG.md) to customize which notifications are sent.

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

## Common Use Cases

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

### Progress Updates

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

## Message Format Reference

### Available Colors

- **Success**: `#2EB887` (green)
- **Error**: `#A30301` (red)
- **Info**: `#3AA3E3` (blue)
- **Warning**: `#FFA500` (orange)

### Automatic Notification Format

The plugin sends messages using Slack's attachment format with Block Kit elements:

- **Workflow Start**: Blue color, includes run name, session ID, command line, and work directory
- **Workflow Complete**: Green color, includes duration, task counts, and resource usage
- **Workflow Error**: Red color, includes error message, failed process, and command line

All automatic messages include:

- Workflow name as the author
- Nextflow icon
- Timestamp footer
- Configurable bot username and icon

## Rate Limiting

The plugin includes built-in rate limiting (max 1 message per second) and retry logic. Best practices:

- Don't send messages in tight loops
- Consider batching notifications
- Use automatic notifications instead of custom messages when possible

## Next Steps

- [Configuration Reference](CONFIG.md) - Full configuration options
- [Examples](EXAMPLES.md) - Progressive examples from basic to advanced
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues and solutions
