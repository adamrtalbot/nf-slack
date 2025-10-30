# Feature Specification: nf-slack Notification Plugin

**Feature Branch**: `001-nf-slack`
**Created**: 2025-10-30
**Status**: Draft
**Input**: User description: "create a Nextflow plugin for creating a Slack messsages as part of the pipeline that does not require a user to modify their pipeline but enables them to send messages by function if required."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic Workflow Notifications (Priority: P1)

A researcher runs a long-running genomics pipeline and wants to be notified when it starts, completes, or fails without modifying their existing workflow code.

**Why this priority**: This is the core value proposition - zero-configuration notifications that work immediately after plugin installation. This is the MVP that delivers immediate value to all users.

**Independent Test**: Can be fully tested by configuring only a webhook URL and running any Nextflow workflow. Success is measured by receiving Slack notifications for workflow start, completion, and error events without any workflow code changes.

**Acceptance Scenarios**:

1. **Given** a Nextflow workflow with nf-slack plugin configured with a webhook URL, **When** the workflow starts, **Then** a Slack notification is sent with workflow name, run ID, and start time
2. **Given** a running workflow with nf-slack configured, **When** the workflow completes successfully, **Then** a Slack notification is sent with completion status, duration, and task statistics
3. **Given** a running workflow with nf-slack configured, **When** the workflow encounters an error, **Then** a Slack notification is sent with error details and failed process information
4. **Given** a Nextflow workflow with nf-slack plugin configured, **When** Slack service is unavailable, **Then** the workflow continues without interruption and logs the notification failure

---

### User Story 2 - Custom In-Workflow Messages (Priority: P2)

A bioinformatician wants to send custom notifications at specific points in their pipeline (e.g., when a critical analysis step completes, when quality thresholds are exceeded) using function calls within their workflow.

**Why this priority**: Extends the basic notification capability to enable fine-grained control. Users who need this will explicitly add it to their workflows, so it's an enhancement rather than core functionality.

**Independent Test**: Can be tested by importing the slackMessage function and calling it from within a workflow or process. Success is measured by receiving custom messages with user-provided content at the expected times.

**Acceptance Scenarios**:

1. **Given** a workflow with slackMessage function imported, **When** the function is called with a text string, **Then** a Slack message is sent with that text content
2. **Given** a workflow using slackMessage, **When** called with custom fields (title, value pairs), **Then** a formatted Slack message is sent displaying those fields
3. **Given** a process that calls slackMessage within its script block, **When** the process executes, **Then** the Slack message is sent at the appropriate point in execution
4. **Given** multiple concurrent processes calling slackMessage, **When** messages are sent simultaneously, **Then** all messages are delivered without rate limit violations or failures

---

### User Story 3 - Notification Customization (Priority: P3)

A pipeline developer wants to customize which events trigger notifications, the message format, and additional metadata included in notifications to match their team's monitoring practices.

**Why this priority**: Provides flexibility for power users and production environments. Basic defaults work for most users, but teams with specific requirements need customization options.

**Independent Test**: Can be tested by modifying configuration options and verifying that notifications respect those settings. Success is measured by receiving only the configured notification types with the specified formatting and metadata.

**Acceptance Scenarios**:

1. **Given** nf-slack configured to disable start notifications, **When** a workflow starts, **Then** no start notification is sent but completion and error notifications still work
2. **Given** custom message templates configured for completion events, **When** a workflow completes, **Then** the notification uses the custom template format
3. **Given** custom fields configured in notification settings, **When** workflow events occur, **Then** notifications include the additional custom fields alongside default information
4. **Given** custom Slack channel configured, **When** notifications are sent, **Then** they appear in the specified channel instead of the webhook default

---

### Edge Cases

- What happens when the webhook URL is invalid or malformed?
- How does the system handle rate limiting from Slack API (maximum messages per minute)?
- What happens when a user calls slackMessage but no webhook is configured?
- How are very long error messages or output handled in notifications (message size limits)?
- What happens when network connectivity is lost during workflow execution?
- How does the plugin behave when multiple workflows run simultaneously in the same environment?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST send Slack notifications on workflow start events without requiring workflow code modifications
- **FR-002**: System MUST send Slack notifications on workflow completion events with duration and success indicators
- **FR-003**: System MUST send Slack notifications on workflow error events with error details and failure context
- **FR-004**: System MUST provide a slackMessage function that can be imported and called from workflows
- **FR-005**: System MUST accept simple text strings as input to slackMessage function
- **FR-006**: System MUST accept structured field data (title-value pairs) as input to slackMessage function
- **FR-007**: System MUST read webhook URL from Nextflow configuration files
- **FR-008**: System MUST support webhook URL configuration via Nextflow secrets mechanism
- **FR-009**: System MUST allow users to selectively enable/disable notifications for start, completion, and error events
- **FR-010**: System MUST allow users to customize notification message templates
- **FR-011**: System MUST include workflow metadata in notifications (run name, session ID, work directory)
- **FR-012**: System MUST include timing information in notifications (start time, duration, completion time)
- **FR-013**: System MUST include resource usage statistics in completion notifications (tasks executed, success/failure counts)
- **FR-014**: System MUST format messages using Slack Block Kit for rich formatting
- **FR-015**: System MUST log all notification attempts and failures for debugging
- **FR-016**: System MUST implement retry logic with exponential backoff for transient Slack API failures
- **FR-017**: System MUST implement rate limiting to respect Slack API limits (1 message per second recommended)
- **FR-018**: System MUST validate webhook URLs at plugin initialization
- **FR-019**: System MUST support custom fields in notifications via configuration
- **FR-020**: System MUST support configuration of Slack bot appearance (username, icon)

### Key Entities

- **Workflow Event**: Represents a workflow lifecycle event (start, completion, error) that triggers a notification, containing metadata like workflow name, timestamp, status, and contextual information
- **Notification Message**: The structured data sent to Slack, including message text, formatting, fields, colors, and metadata
- **Webhook Configuration**: The connection settings for Slack integration, including webhook URL, channel, bot appearance, and enabled notification types
- **Custom Message**: User-defined messages sent via slackMessage function, containing either simple text or structured field data

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can receive workflow notifications in Slack within 5 seconds of workflow events occurring
- **SC-002**: Users can enable the plugin and receive notifications by adding only a webhook URL to their configuration (less than 2 minutes setup time)
- **SC-003**: Plugin handles at least 100 concurrent workflow executions without message loss or notification failures
- **SC-004**: 95% of notification send attempts succeed on first try under normal conditions
- **SC-005**: Custom messages sent via slackMessage function appear in Slack within 3 seconds of function call
- **SC-006**: Plugin configuration changes take effect immediately without requiring workflow restarts
- **SC-007**: Users can customize message templates and see changes reflected in the next notification
- **SC-008**: Zero workflow failures caused by plugin errors or Slack communication issues
- **SC-009**: Complete notification delivery even when Slack API response time degrades up to 5 seconds
- **SC-010**: Users report 90% satisfaction with default notification content and formatting

## Assumptions

- Slack webhook URLs follow standard Slack incoming webhook format (https://hooks.slack.com/services/...)
- Users have appropriate permissions to create Slack webhooks in their workspaces
- Network connectivity exists between Nextflow execution environment and Slack API (HTTPS outbound)
- Nextflow version supports the plugin API version required for event hooks
- Default message formatting follows common Slack notification patterns used in CI/CD tools
- Error notifications will include last 500 characters of error output (truncated if longer)
- Completion notifications include basic resource metrics available from Nextflow TraceRecord
- Rate limiting of 1 message per second is sufficient for most workflow notification patterns
- Webhook URL is the only required configuration parameter for basic functionality
