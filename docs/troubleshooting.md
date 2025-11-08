# Troubleshooting Guide

Common issues and solutions for nf-slack.

## No Messages Received

### Check Plugin is Enabled

1. Verify plugin is installed:

   ```bash
   nextflow plugin list
   ```

2. Check your config has `enabled = true`:

   ```groovy
   slack {
       enabled = true
       webhook {
           url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
       }
   }
   ```

3. Verify at least one notification type is enabled (they're all enabled by default):
   ```groovy
   slack {
       onStart.enabled = true
       onComplete.enabled = true
       onError.enabled = true
   }
   ```

### Check Webhook URL

1. **Verify format**: URL should start with `https://hooks.slack.com/services/`

2. **Test webhook directly** using curl:

   ```bash
   curl -X POST -H 'Content-type: application/json' \
     --data '{"text":"Test message"}' \
     YOUR_WEBHOOK_URL
   ```

   If this fails, your webhook URL is invalid or has been revoked.

3. **Common issues**:
   - URL contains typos
   - Webhook was deleted in Slack
   - Wrong workspace webhook

### Check Nextflow Logs

Look for "Slack plugin:" messages in the Nextflow log output:

```bash
nextflow run your_pipeline.nf 2>&1 | grep -i slack
```

Common log messages:

- `Slack plugin: enabled` - Plugin is active
- `Slack plugin: disabled` - Plugin is not active (check config)
- `Failed to send Slack message` - Network or webhook issue
- `Invalid webhook URL` - Webhook URL format is wrong

## Messages Not Formatted Correctly

### Rich Message Format Issues

Ensure you're using the correct structure:

```groovy
slack {
    onStart {
        message = [
            text: 'Message text',      // String
            color: '#3AA3E3',           // Hex color string
            includeFields: ['runName'], // List of field names
            customFields: [             // List of field maps
                [title: 'Label', value: 'Content', short: true]
            ]
        ]
    }
}
```

### Common Formatting Mistakes

1. **Invalid color codes**:

   - ❌ `color: 'blue'`
   - ✅ `color: '#3AA3E3'`

2. **Wrong field structure**:

   - ❌ `fields: ['title', 'value']`
   - ✅ `fields: [[title: 'Title', value: 'Value']]`

3. **Missing quotes**:
   - ❌ `includeFields: [runName, status]`
   - ✅ `includeFields: ['runName', 'status']`

### Short Fields Layout

Fields with `short: true` should come in pairs for proper column layout:

```groovy
customFields: [
    [title: 'Field 1', value: 'Value 1', short: true],
    [title: 'Field 2', value: 'Value 2', short: true],  // These two appear side-by-side
    [title: 'Field 3', value: 'Value 3', short: false]  // This appears full-width
]
```

## Plugin Not Loading

### Installation Issues

1. **Clean and reinstall**:

   ```bash
   make clean
   make install
   ```

2. **Check plugin version** matches in config:

   ```groovy
   plugins {
       id 'nf-slack@0.1.0'  // Must match installed version
   }
   ```

3. **Verify installation location**:
   ```bash
   ls ~/.nextflow/plugins/
   ```

### Configuration File Issues

1. **Check config syntax**:

   ```bash
   nextflow config -profile your_profile
   ```

2. **Common syntax errors**:
   - Missing commas in lists
   - Unclosed brackets
   - Invalid characters in strings

## Custom Messages Not Working

### Plugin Must Be Configured

Custom messages only work if the plugin is enabled and has a valid webhook:

```groovy
slack {
    enabled = true
    webhook {
        url = 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    }
}
```

### Import Statement Required

Make sure you include the function:

```groovy
include { slackMessage } from 'plugin/nf-slack'
```

### Check Message Format

For simple messages:

```groovy
slackMessage("Text message")
```

For rich messages:

```groovy
slackMessage([
    message: "Text",
    color: "#2EB887",
    fields: [
        [title: "Title", value: "Value", short: true]
    ]
])
```

## Rate Limiting Issues

### Symptoms

- Messages are delayed
- Some messages are not sent
- "Too many requests" errors in logs

### Solutions

1. **Reduce message frequency**:

   - Don't send messages in tight loops
   - Batch related updates into single messages

2. **Use built-in throttling**:
   The plugin automatically limits to 1 message per second

3. **Check Slack workspace limits**:
   - Free workspaces have stricter rate limits
   - Consider upgrading for high-volume usage

## Network Issues

### Connection Timeouts

If messages fail due to network issues:

1. **Check network connectivity**:

   ```bash
   curl -I https://hooks.slack.com
   ```

2. **Check proxy settings** if behind corporate firewall

3. **Verify firewall rules** allow outbound HTTPS connections

### SSL/TLS Issues

If you see SSL certificate errors:

1. Update Java certificates:

   ```bash
   java -version  # Check Java version
   # Update Java to latest patch version
   ```

2. Check system time is correct (SSL certificates are time-sensitive)

## Getting Help

If you're still having issues:

1. **Enable debug logging**:

   ```bash
   NXF_DEBUG=1 nextflow run your_pipeline.nf
   ```

2. **Collect information**:

   - Nextflow version: `nextflow -version`
   - Plugin version: `nextflow plugin list`
   - Configuration (with webhook URL redacted)
   - Full error messages from logs

3. **Report an issue**:
   - [GitHub Issues](https://github.com/adamrtalbot/nf-slack/issues)
   - Include all collected information
   - Provide minimal reproducible example

## Related Documentation

- [API Reference](reference/api.md) - Full configuration options
- [Usage Guide](usage/custom-messages.md) - How to use the plugin
- [Examples](examples/gallery.md) - Working examples
