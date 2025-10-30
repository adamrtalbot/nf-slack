# Research: nf-slack Notification Plugin

**Feature**: nf-slack Notification Plugin
**Date**: 2025-10-30
**Purpose**: Research technical decisions and best practices for implementing Slack notifications in a Nextflow plugin

## Overview

This document captures research findings and technical decisions for implementing the nf-slack plugin. Since the user specified using "Nextflow plugin template to implement Slack messages using the REST endpoint with webhook URL and Groovy for code," the core technical stack is predetermined. This research focuses on best practices, patterns, and implementation details within those constraints.

## Technology Decisions

### Decision 1: Nextflow Plugin API - TraceObserver Extension Point

**Decision**: Use Nextflow's `TraceObserver` interface to hook into workflow lifecycle events.

**Rationale**:
- TraceObserver is the standard Nextflow plugin mechanism for monitoring workflow events
- Provides callbacks for `onFlowStart`, `onFlowComplete`, `onFlowError` events
- Non-intrusive - doesn't require users to modify workflow code
- Already implemented in the existing codebase (SlackObserver.groovy exists)

**Alternatives Considered**:
- **Custom event system**: Would require workflow code changes, violating FR-001 requirement
- **File watching**: Too fragile and platform-dependent

**Implementation Notes**:
- SlackObserver implements `TraceObserver` interface
- Registered via SlackFactory during plugin initialization
- Events are delivered synchronously but HTTP calls should be async

### Decision 2: Nextflow Plugin API - FunctionExtension for slackMessage()

**Decision**: Use Nextflow's `FunctionExtension` interface to provide the custom `slackMessage()` function.

**Rationale**:
- FunctionExtension is the standard way to add custom functions to Nextflow DSL
- Functions become available via `include` statement
- Type-safe Groovy function signatures
- Already implemented in existing codebase (SlackExtension.groovy exists)

**Alternatives Considered**:
- **Process script**: Would require explicit process definitions, less ergonomic for users
- **Workflow-level only**: FunctionExtension allows use in both workflows and processes

**Implementation Notes**:
- SlackExtension provides `slackMessage(String)` and `slackMessage(Map)` overloads
- Function validates config and delegates to SlackClient
- Must handle case where webhook is not configured gracefully

### Decision 3: Slack Incoming Webhooks API

**Decision**: Use Slack's Incoming Webhooks REST API with Block Kit formatting.

**Rationale**:
- Incoming Webhooks are the simplest Slack integration method (no OAuth, no app installation beyond webhook creation)
- Block Kit provides rich formatting capabilities (colors, fields, structured layout)
- HTTPS-only, built-in security
- User input specified "REST endpoint with webhook URL"

**Alternatives Considered**:
- **Slack Web API**: Requires OAuth tokens, app installation, more complex setup (violates User-First Design principle)
- **Slack Socket Mode**: Requires WebSocket connections, unnecessary complexity for one-way notifications

**API Contract**:
```
POST https://hooks.slack.com/services/{workspace}/{channel}/{token}
Content-Type: application/json

{
  "text": "Fallback text",
  "attachments": [{
    "color": "#36a64f",
    "author_name": "Workflow Name",
    "title": "Message Title",
    "fields": [
      {"title": "Field", "value": "Value", "short": true}
    ],
    "footer": "Nextflow",
    "ts": 1234567890
  }]
}
```

### Decision 4: Groovy HTTPBuilder for HTTP Client

**Decision**: Use Groovy's built-in HTTP client capabilities (HttpURLConnection wrapped in Groovy convenience methods).

**Rationale**:
- No additional dependencies required
- Groovy provides excellent HTTP client DSL
- Sufficient for simple POST requests
- Already used in existing SlackClient.groovy implementation

**Alternatives Considered**:
- **Apache HttpComponents**: Additional dependency, unnecessary for simple POST
- **OkHttp**: Modern but adds dependency weight
- **Groovy's HttpBuilder-NG**: Nice API but extra dependency

**Implementation Notes**:
- SlackClient handles URL validation, request building, response parsing
- Implements retry logic with exponential backoff
- Rate limiting via synchronized method or semaphore

### Decision 5: Asynchronous Message Sending

**Decision**: Send Slack messages asynchronously using Groovy's GPars or Java ExecutorService.

**Rationale**:
- Must not block workflow execution (Performance quality standard)
- Slack API latency should not impact workflow timing
- Allows retry logic without holding up event processing

**Alternatives Considered**:
- **Synchronous blocking**: Violates performance constraints, could delay workflow events
- **Fire-and-forget threads**: No control over concurrency, resource management issues

**Implementation Notes**:
- Use bounded thread pool (e.g., 2-4 threads) to limit concurrency
- Queue messages if Slack is slow/unavailable
- Log failures but don't propagate exceptions to workflow

### Decision 6: Configuration via Nextflow Config DSL

**Decision**: Use Nextflow's standard `config` DSL with a `slack { }` configuration block.

**Rationale**:
- Consistent with all other Nextflow plugins
- Supports both simple and complex configurations
- Built-in support for secrets management
- SlackConfig.groovy already implements this pattern

**Configuration Structure**:
```groovy
slack {
    enabled = true
    webhook = 'https://hooks.slack.com/services/...'  // or secrets.SLACK_WEBHOOK

    // Simple string format
    startMessage = '🚀 Pipeline started'

    // Or map format for advanced customization
    startMessage = [
        text: '🚀 Pipeline started',
        color: '#3AA3E3',
        includeFields: ['runName', 'status', 'commandLine'],
        customFields: [
            [title: 'Environment', value: 'Production', short: true]
        ]
    ]

    // Control which events trigger notifications
    notifyOnStart = true
    notifyOnComplete = true
    notifyOnError = true
}
```

### Decision 7: Rate Limiting Strategy

**Decision**: Implement token bucket rate limiter allowing 1 message per second with burst capacity of 3.

**Rationale**:
- Slack recommends 1 message/second for Incoming Webhooks
- Burst capacity handles concurrent workflow completions
- Simple to implement with AtomicLong timestamp tracking

**Implementation Notes**:
- SlackClient checks timestamp before sending
- If rate exceeded, sleep briefly then retry
- Log rate limit encounters for monitoring

### Decision 8: Error Handling and Retry Logic

**Decision**: Implement exponential backoff retry for HTTP 429, 5xx errors; fail fast for 4xx errors.

**Rationale**:
- 429 (rate limit) and 5xx (server errors) are transient - worth retrying
- 4xx (client errors like 404, 401) indicate configuration problems - retrying won't help
- Exponential backoff prevents hammering Slack API

**Retry Schedule**:
- Initial delay: 1 second
- Max retries: 3
- Backoff multiplier: 2x (1s, 2s, 4s)
- Total max delay: 7 seconds

**Implementation Notes**:
- SlackClient implements retry logic
- Log all attempts and outcomes
- After max retries, log error but don't throw exception

## Best Practices Research

### Nextflow Plugin Development

**Key Patterns**:
1. **Plugin Registration**: Extend `BasePlugin`, return factory from `getFactory()`
2. **Configuration**: Use `@ConfigAttribute` annotations for type-safe config parsing
3. **Testing**: Spock framework with `@Specification` base class for BDD-style tests
4. **Resources**: META-INF/MANIFEST.MF declares plugin class and extensions

**References**:
- Existing nf-slack codebase already follows these patterns
- Nextflow plugin template provides structure

### Slack Block Kit Best Practices

**Formatting Guidelines**:
1. **Colors**: Use semantic colors (green=success #2EB887, red=error #A30301, blue=info #3AA3E3)
2. **Fields**: Use `short: true` for label-value pairs that should appear side-by-side
3. **Text**: Support Slack markdown (*bold*, _italic_, `code`, links)
4. **Fallback**: Always provide `text` field for notifications/search

**Message Structure**:
```groovy
[
    color: colorCode,
    author_name: workflowName,
    author_icon: nextflowIconUrl,
    title: eventTitle,
    fields: [
        [title: 'Run Name', value: runName, short: true],
        [title: 'Status', value: status, short: true]
    ],
    footer: 'Nextflow',
    ts: timestamp
]
```

### Groovy HTTP Client Patterns

**Best Practices**:
```groovy
def sendMessage(String webhookUrl, Map payload) {
    def connection = new URL(webhookUrl).openConnection() as HttpURLConnection
    connection.setRequestMethod('POST')
    connection.setRequestProperty('Content-Type', 'application/json')
    connection.setDoOutput(true)

    connection.outputStream.withWriter('UTF-8') { writer ->
        writer.write(JsonOutput.toJson(payload))
    }

    def responseCode = connection.responseCode
    if (responseCode == 200) {
        return true
    } else {
        def error = connection.errorStream?.text
        throw new IOException("Slack API error: ${responseCode} - ${error}")
    }
}
```

### Testing Strategies

**Unit Testing**:
- Mock SlackClient in SlackObserver tests
- Test configuration parsing with various inputs
- Test message builder with different field combinations

**Integration Testing**:
- Use WireMock or similar to mock Slack API
- Test retry logic with simulated failures
- Test rate limiting with concurrent requests

**Example Configuration Testing**:
- Validate each example config file parses correctly
- Ensure progressive examples build on each other logically

## Security Considerations

### Webhook URL Protection

**Issue**: Webhook URLs contain secrets in the URL path.

**Mitigations**:
1. Support Nextflow secrets: `webhook = secrets.SLACK_WEBHOOK`
2. Never log webhook URLs in error messages
3. Validate URLs start with `https://hooks.slack.com/services/`
4. Mask URLs in any debug output: `https://hooks.slack.com/services/***`

### Rate Limiting Abuse Prevention

**Issue**: Malicious config could spam Slack.

**Mitigations**:
1. Hard rate limit of 1 message/second enforced in code
2. Bounded thread pool prevents resource exhaustion
3. Max message size limits (Slack enforces 4000 char limit)

### HTTPS Enforcement

**Issue**: Plain HTTP would expose webhook tokens.

**Mitigation**:
- Validate webhook URLs use HTTPS scheme only
- Reject HTTP URLs at configuration validation time

## Implementation Roadmap

Based on this research, the implementation follows this order:

1. **Configuration Layer** (SlackConfig.groovy)
   - Parse simple string and map-based message configs
   - Validate webhook URL format and HTTPS
   - Support Nextflow secrets integration

2. **HTTP Client Layer** (SlackClient.groovy)
   - HTTP POST with JSON payload
   - Retry logic with exponential backoff
   - Rate limiting implementation

3. **Message Builder Layer** (SlackMessageBuilder.groovy)
   - Format Block Kit attachments
   - Handle custom fields and default fields
   - Apply colors and formatting

4. **Event Observer Layer** (SlackObserver.groovy)
   - Hook TraceObserver events
   - Extract workflow metadata
   - Delegate to SlackClient asynchronously

5. **Function Extension Layer** (SlackExtension.groovy)
   - Provide slackMessage() function
   - Handle string and map signatures
   - Validate configuration exists

6. **Testing Layer**
   - Unit tests for each component
   - Integration tests for HTTP client
   - Example config validation

7. **Documentation Layer**
   - Progressive example configs
   - README with quick start
   - Inline code documentation

## Unresolved Questions

None - all technical decisions are clear based on user input and existing codebase analysis.
