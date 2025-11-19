# API Contract: Slack Bot API

**Feature**: nf-slack Notification Plugin (Bot Integration)
**Date**: 2025-11-19
**Purpose**: Define the contract for interaction with Slack's Web API (Bot User)

## Overview

This document specifies the interaction between the nf-slack plugin and Slack's Web API using a Bot User OAuth Token. This allows the plugin to send messages as a specific app/bot user rather than a generic webhook.

## Endpoint

**Base URL**: `https://slack.com/api/chat.postMessage`
**Method**: `POST`
**Content-Type**: `application/json; charset=utf-8`
**Authorization**: `Bearer xoxb-...` (Bot User OAuth Token)

## Configuration

The plugin requires the following configuration for Bot integration:

```groovy
slack {
    bot {
        token = 'xoxb-your-bot-token'
        channel = '#general' // Default channel ID or name
    }
}
```

## Request Format

### Headers

```http
POST /api/chat.postMessage HTTP/1.1
Host: slack.com
Authorization: Bearer xoxb-1234-5678-abcdef
Content-Type: application/json; charset=utf-8
```

### Request Body Schema

```json
{
  "channel": "string (required)",
  "text": "string (required)",
  "blocks": [ ... ] (optional, Block Kit),
  "attachments": [ ... ] (optional, legacy),
  "thread_ts": "string (optional)",
  "mrkdwn": boolean (optional, default: true)
}
```

### Field Constraints

- `channel`: Channel ID (e.g., `C12345678`) is preferred over channel name (`#general`).
- `text`: Fallback text or main message.
- `blocks`: Array of layout blocks (preferred over attachments).
- `attachments`: Legacy array of attachments (supported for backward compatibility with webhook payloads).

## Response Format

### Success Response (200 OK)

```json
{
    "ok": true,
    "channel": "C12345678",
    "ts": "1503435956.000247",
    "message": { ... }
}
```

### Error Response (200 OK with error)

Slack Web API returns 200 OK even for most errors, with `ok: false`.

```json
{
  "ok": false,
  "error": "channel_not_found"
}
```

**Common Errors**:

- `channel_not_found`: Channel doesn't exist or bot not in channel.
- `not_in_channel`: Bot is not a member of the private channel.
- `invalid_auth`: Invalid token.
- `account_inactive`: Bot user disabled.
- `rate_limited`: Too many requests (check `Retry-After` header).

## Implementation Details

### BotSlackSender

The `BotSlackSender` class will implement `SlackSender` and handle:

1.  Constructing the JSON payload.
2.  Adding the `Authorization` header.
3.  Sending the request to `chat.postMessage`.
4.  Parsing the JSON response to check `ok`.
5.  Handling errors and retries.

### Authentication

- Token must start with `xoxb-`.
- Token should be treated as a secret (support Nextflow secrets).

## Verification

- **Unit Tests**: Mock Slack API responses (success, error, rate limit).
- **Integration Tests**: Send real message to a test channel using a test bot token.
