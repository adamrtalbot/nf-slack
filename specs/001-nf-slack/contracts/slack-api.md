# API Contract: Slack Incoming Webhooks

**Feature**: nf-slack Notification Plugin
**Date**: 2025-10-30
**Purpose**: Define the contract for interaction with Slack's Incoming Webhooks API

## Overview

This document specifies the HTTP API contract between the nf-slack plugin and Slack's Incoming Webhooks REST API. The plugin acts as an HTTP client, sending formatted messages via POST requests.

## Endpoint

**Base URL**: User-provided webhook URL
**Format**: `https://hooks.slack.com/services/{workspace_id}/{channel_id}/{token}`
**Method**: `POST`
**Content-Type**: `application/json`

### URL Validation Rules

The plugin MUST validate webhook URLs before use:
- ✅ Scheme must be `https` (not `http`)
- ✅ Host must be `hooks.slack.com`
- ✅ Path must match pattern `/services/{workspace}/{channel}/{token}`
- ❌ Reject any URL not matching this pattern

### Example Valid URLs

```
https://hooks.slack.com/services/{WORKSPACE_ID}/{CHANNEL_ID}/{SECRET_TOKEN}
https://hooks.slack.com/services/T1A2B3C4D/B5E6F7G8H/example-token-1a2b3c4d5e6f7g8h
```

### Example Invalid URLs

```
http://hooks.slack.com/services/...          # HTTP not HTTPS
https://other-domain.com/webhook            # Wrong domain
https://hooks.slack.com/other-path          # Wrong path pattern
```

## Request Format

### Headers

**Required Headers**:
```http
POST /services/{workspace}/{channel}/{token} HTTP/1.1
Host: hooks.slack.com
Content-Type: application/json
Content-Length: {payload_size}
User-Agent: nf-slack/{version}
```

### Request Body Schema

**JSON Structure** (application/json):
```json
{
  "text": "string (required)",
  "username": "string (optional)",
  "icon_emoji": "string (optional)",
  "icon_url": "string (optional)",
  "channel": "string (optional)",
  "attachments": [
    {
      "fallback": "string (optional)",
      "color": "string (optional)",
      "pretext": "string (optional)",
      "author_name": "string (optional)",
      "author_link": "string (optional)",
      "author_icon": "string (optional)",
      "title": "string (optional)",
      "title_link": "string (optional)",
      "text": "string (optional)",
      "fields": [
        {
          "title": "string (required)",
          "value": "string (required)",
          "short": boolean (optional, default: false)
        }
      ],
      "image_url": "string (optional)",
      "thumb_url": "string (optional)",
      "footer": "string (optional)",
      "footer_icon": "string (optional)",
      "ts": integer (optional, unix timestamp)
    }
  ]
}
```

### Field Constraints

**Top-Level Fields**:
- `text` (string, required): Fallback text, max 4000 characters
- `username` (string, optional): Bot display name, max 80 characters
- `icon_emoji` (string, optional): Emoji code like `:rocket:`
- `icon_url` (string, optional): URL to bot icon image
- `channel` (string, optional): Override webhook default channel (e.g., `#alerts`, `@user`)

**Attachment Fields**:
- `color` (string, optional): Hex color code `#RRGGBB` or preset (`good`, `warning`, `danger`)
- `author_name` (string, optional): Author/workflow name, max 256 characters
- `title` (string, optional): Message title, max 256 characters
- `text` (string, optional): Main message body, max 8000 characters
- `fields` (array, optional): Structured field list, max 20 fields
- `footer` (string, optional): Footer text, max 300 characters
- `ts` (integer, optional): Unix timestamp in seconds

**Field Object**:
- `title` (string, required): Field label, max 50 characters
- `value` (string, required): Field content, max 2000 characters
- `short` (boolean, optional): Display in column layout if true

### Size Limits

- Total JSON payload: **4 KB maximum** (Slack enforced)
- Individual field limits as noted above
- Plugin SHOULD truncate error messages and long text to stay under limits

## Request Examples

### Minimal Request (Simple Text)

```json
{
  "text": "🚀 Pipeline started"
}
```

### Workflow Start Notification

```json
{
  "text": "Pipeline started",
  "username": "Nextflow Bot",
  "icon_emoji": ":rocket:",
  "attachments": [{
    "color": "#3AA3E3",
    "author_name": "RNA-seq Pipeline",
    "author_icon": "https://www.nextflow.io/icon.png",
    "title": "🚀 Pipeline started",
    "fields": [
      {"title": "Run Name", "value": "focused_euler", "short": true},
      {"title": "Status", "value": "🟢 Running", "short": true},
      {"title": "Command Line", "value": "nextflow run main.nf --input data/", "short": false},
      {"title": "Work Directory", "value": "/work/abc123", "short": false}
    ],
    "footer": "Nextflow",
    "footer_icon": "https://www.nextflow.io/icon.png",
    "ts": 1730300400
  }]
}
```

### Workflow Completion Notification

```json
{
  "text": "Pipeline completed successfully",
  "username": "Nextflow Bot",
  "icon_emoji": ":rocket:",
  "attachments": [{
    "color": "#2EB887",
    "author_name": "RNA-seq Pipeline",
    "author_icon": "https://www.nextflow.io/icon.png",
    "title": "✅ Pipeline completed successfully",
    "fields": [
      {"title": "Run Name", "value": "focused_euler", "short": true},
      {"title": "Duration", "value": "1h 23m 45s", "short": true},
      {"title": "Status", "value": "✅ Completed", "short": true},
      {"title": "Tasks", "value": "42 succeeded, 0 failed, 5 cached", "short": true}
    ],
    "footer": "Nextflow",
    "footer_icon": "https://www.nextflow.io/icon.png",
    "ts": 1730304000
  }]
}
```

### Workflow Error Notification

```json
{
  "text": "Pipeline failed",
  "username": "Nextflow Bot",
  "icon_emoji": ":rocket:",
  "attachments": [{
    "color": "#A30301",
    "author_name": "RNA-seq Pipeline",
    "author_icon": "https://www.nextflow.io/icon.png",
    "title": "❌ Pipeline failed",
    "fields": [
      {"title": "Run Name", "value": "focused_euler", "short": true},
      {"title": "Duration", "value": "23m 12s", "short": true},
      {"title": "Failed Process", "value": "ALIGN_READS", "short": true},
      {"title": "Error Message", "value": "Command error: samtools index failed with exit status 1", "short": false}
    ],
    "footer": "Nextflow",
    "footer_icon": "https://www.nextflow.io/icon.png",
    "ts": 1730302000
  }]
}
```

### Custom User Message with Fields

```json
{
  "text": "Analysis results available",
  "username": "Nextflow Bot",
  "icon_emoji": ":rocket:",
  "attachments": [{
    "color": "#2EB887",
    "author_name": "RNA-seq Pipeline",
    "author_icon": "https://www.nextflow.io/icon.png",
    "title": "Analysis Results",
    "fields": [
      {"title": "Sample", "value": "sample_001", "short": true},
      {"title": "Status", "value": "✅ Success", "short": true},
      {"title": "Total Variants", "value": "1,234", "short": true},
      {"title": "Quality Score", "value": "98.5%", "short": true},
      {"title": "Results Path", "value": "s3://bucket/results/sample_001/", "short": false}
    ],
    "footer": "Nextflow",
    "ts": 1730303000
  }]
}
```

## Response Format

### Success Response (200 OK)

```http
HTTP/1.1 200 OK
Content-Type: text/plain
Content-Length: 2

ok
```

**Body**: Literal string `"ok"`

**Interpretation**: Message accepted and queued for delivery

### Error Responses

#### 400 Bad Request

```http
HTTP/1.1 400 Bad Request
Content-Type: text/plain

invalid_payload
```

**Causes**:
- Malformed JSON
- Missing required `text` field
- Invalid field types
- Payload exceeds size limits

**Plugin Behavior**: Log error, do NOT retry (client error)

#### 403 Forbidden

```http
HTTP/1.1 403 Forbidden
Content-Type: text/plain

action_prohibited
```

**Causes**:
- Invalid webhook URL
- Webhook has been revoked
- Workspace access restrictions

**Plugin Behavior**: Log error, do NOT retry (client error)

#### 404 Not Found

```http
HTTP/1.1 404 Not Found
Content-Type: text/plain

channel_not_found
```

**Causes**:
- Webhook URL no longer exists
- Channel has been deleted

**Plugin Behavior**: Log error, do NOT retry (client error)

#### 410 Gone

```http
HTTP/1.1 410 Gone
Content-Type: text/plain

channel_is_archived
```

**Causes**:
- Target channel has been archived

**Plugin Behavior**: Log error, do NOT retry (permanent error)

#### 429 Too Many Requests

```http
HTTP/1.1 429 Too Many Requests
Content-Type: text/plain
Retry-After: 1

rate_limited
```

**Causes**:
- Exceeded Slack rate limits (typically 1 message/second)

**Plugin Behavior**: Retry with exponential backoff, respect `Retry-After` header

#### 500 Internal Server Error

```http
HTTP/1.1 500 Internal Server Error
Content-Type: text/plain

rollup_error
```

**Causes**:
- Slack internal server error
- Transient service issue

**Plugin Behavior**: Retry with exponential backoff (max 3 attempts)

#### 503 Service Unavailable

```http
HTTP/1.1 503 Service Unavailable
Content-Type: text/plain

service_unavailable
```

**Causes**:
- Slack service temporarily down
- Maintenance window

**Plugin Behavior**: Retry with exponential backoff (max 3 attempts)

## Retry Policy

The plugin MUST implement the following retry policy:

### Retry on Transient Errors

**Retry these status codes**:
- 429 (Too Many Requests)
- 500 (Internal Server Error)
- 503 (Service Unavailable)
- Network errors (connection timeout, connection refused)

**Retry Schedule**:
```
Attempt 1: Immediate
Attempt 2: Wait 1 second
Attempt 3: Wait 2 seconds
Attempt 4: Wait 4 seconds
Max: 3 retries (4 total attempts)
```

**Honor `Retry-After` Header**:
- If present on 429 response, wait specified seconds before retry
- Override default backoff schedule

### Do NOT Retry Client Errors

**Never retry these status codes**:
- 400 (Bad Request) - Invalid payload
- 403 (Forbidden) - Authentication/authorization issue
- 404 (Not Found) - Resource doesn't exist
- 410 (Gone) - Resource permanently deleted

These indicate configuration or permanent errors that won't resolve with retries.

## Rate Limiting

**Slack Recommendation**: 1 message per second per webhook

**Plugin Implementation**:
- Enforce 1 message/second limit internally
- Use token bucket or timestamp-based rate limiter
- Queue messages if sending too fast
- Log rate limit encounters for monitoring

**Burst Handling**:
- Allow burst of up to 3 messages
- Then enforce 1/second rate
- Prevents dropped messages during workflow startup when multiple events fire rapidly

## Error Handling Strategy

### Network Errors

```
Connection Timeout (> 10 seconds)
  -> Log warning
  -> Retry with backoff
  -> After max retries: Log error, continue workflow

Connection Refused
  -> Log error
  -> Do NOT retry (indicates DNS or network config issue)
  -> Continue workflow

SSL/TLS Errors
  -> Log error
  -> Do NOT retry (indicates certificate or security issue)
  -> Continue workflow
```

### Plugin Behavior on Failure

**Critical**: The plugin MUST NEVER cause workflow failure due to Slack issues.

**On all errors after retry exhaustion**:
1. Log detailed error message (including sanitized webhook URL)
2. Continue workflow execution normally
3. Emit warning to Nextflow log
4. Do NOT throw exceptions to Nextflow runtime

## Security Considerations

### Webhook URL Protection

- Never log full webhook URL (contains secret token)
- Mask URLs in logs: `https://hooks.slack.com/services/***`
- Support Nextflow secrets for webhook configuration
- Validate HTTPS scheme strictly

### Request Security

- Always use HTTPS (enforced by URL validation)
- Set User-Agent header for tracking
- Do NOT follow redirects (potential security risk)
- Validate response content type and size

### Data Sanitization

- Escape special characters in user-provided text
- Truncate overly long fields to prevent payload bloat
- Validate custom field data types
- Do NOT include sensitive data (tokens, passwords) in messages

## Testing Recommendations

### Unit Tests

- Mock HTTP responses for all status codes
- Test retry logic with simulated failures
- Verify rate limiting behavior
- Validate payload serialization

### Integration Tests

- Use real webhook URL in test environment
- Verify message delivery end-to-end
- Test timeout handling with slow responses
- Validate error scenarios (invalid URL, 4xx errors)

### Contract Tests

- Verify request payload matches Slack schema
- Test all example payloads parse correctly
- Validate field size limits enforced
- Check total payload size under 4KB

## Changes from Previous Versions

N/A - Initial version

## References

- [Slack Incoming Webhooks Documentation](https://api.slack.com/messaging/webhooks)
- [Slack Block Kit Builder](https://api.slack.com/block-kit)
- [Slack Message Formatting](https://api.slack.com/reference/surfaces/formatting)
- [Slack Rate Limiting](https://api.slack.com/docs/rate-limits)
