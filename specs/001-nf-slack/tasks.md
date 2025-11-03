# Tasks: nf-slack Notification Plugin

**Input**: Design documents from `/specs/001-nf-slack/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/slack-api.md, quickstart.md

**Tests**: Tests are NOT explicitly requested in the specification. However, existing test files (SlackConfigTest.groovy, SlackClientTest.groovy, SlackMessageBuilderTest.groovy, SlackObserverTest.groovy) provide coverage for public APIs per Constitution Principle III. Implementation tasks assume these tests are maintained and extended concurrently with code changes. Phase 7 includes validation tasks to verify test coverage completeness.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

This is a Nextflow plugin project with standard Groovy structure:

- **Main source**: `src/main/groovy/nextflow/slack/`
- **Test source**: `src/test/groovy/nextflow/slack/`
- **Examples**: `example/configs/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure (already exists, verify completeness)

- [x] T001 Verify Gradle build configuration includes all required dependencies in build.gradle
- [x] T002 [P] Verify plugin manifest exists at src/main/resources/META-INF/MANIFEST.MF
- [x] T003 [P] Verify project structure matches plan.md (src/main/groovy/nextflow/slack/ directory)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Implement SlackConfig configuration parser in src/main/groovy/nextflow/slack/SlackConfig.groovy
- [x] T005 [P] Implement SlackClient HTTP client with basic POST in src/main/groovy/nextflow/slack/SlackClient.groovy
- [x] T006 [P] Implement SlackMessageBuilder for Block Kit formatting in src/main/groovy/nextflow/slack/SlackMessageBuilder.groovy
- [ ] T007 Add webhook URL validation (HTTPS enforcement, format checking) to SlackConfig
- [ ] T008 Add retry logic with exponential backoff to SlackClient (429, 5xx errors)
- [ ] T009 Add rate limiting implementation (1 msg/sec with burst of 3) to SlackClient
- [ ] T010 Add asynchronous message sending with ExecutorService to SlackClient
- [x] T011 Implement error handling and logging throughout SlackClient

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Automatic Workflow Notifications (Priority: P1) 🎯 MVP

**Goal**: Users receive automatic Slack notifications for workflow start, completion, and error events without modifying workflow code

**Independent Test**: Configure only a webhook URL in nextflow.config and run any Nextflow workflow. Verify Slack notifications appear for start, completion, and error events.

### Implementation for User Story 1

- [x] T012 [P] [US1] Implement SlackObserver implementing TraceObserver interface in src/main/groovy/nextflow/slack/SlackObserver.groovy
- [x] T013 [P] [US1] Implement SlackFactory for plugin registration in src/main/groovy/nextflow/slack/SlackFactory.groovy
- [x] T014 [US1] Implement onFlowStart event handler in SlackObserver (extract workflow metadata, call SlackMessageBuilder)
- [x] T015 [US1] Implement onFlowComplete event handler in SlackObserver (extract duration, task stats, call SlackMessageBuilder)
- [x] T016 [US1] Implement onFlowError event handler in SlackObserver (extract error details, failed process, call SlackMessageBuilder)
- [x] T017 [US1] Add workflow start message formatting to SlackMessageBuilder (blue color, runName, sessionId, commandLine, workDir fields)
- [x] T018 [US1] Add workflow completion message formatting to SlackMessageBuilder (green color, runName, duration, task statistics fields)
- [x] T019 [US1] Add workflow error message formatting to SlackMessageBuilder (red color, runName, errorMessage, failedProcess fields)
- [x] T020 [US1] Wire SlackObserver registration in SlackFactory.create() method
- [x] T021 [US1] Add Slack service unavailability handling (log error, continue workflow) in SlackObserver
- [x] T022 [US1] Update SlackPlugin main entry point to register SlackFactory in src/main/groovy/nextflow/slack/SlackPlugin.groovy

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently. Users can receive automatic notifications for all workflow events.

---

## Phase 4: User Story 2 - Custom In-Workflow Messages (Priority: P2)

**Goal**: Users can send custom Slack notifications from within workflows using slackMessage() function

**Independent Test**: Import slackMessage function in a workflow and call it with text or fields. Verify custom messages appear in Slack at the expected times.

### Implementation for User Story 2

- [x] T023 [P] [US2] Implement SlackExtension implementing FunctionExtension interface in src/main/groovy/nextflow/slack/SlackExtension.groovy
- [x] T024 [US2] Implement slackMessage(String text) function overload in SlackExtension
- [x] T025 [US2] Implement slackMessage(Map params) function overload in SlackExtension
- [x] T026 [US2] Add validation for slackMessage() parameters (text required, fields structure validation)
- [x] T027 [US2] Add check for webhook configuration before sending custom messages (graceful error if not configured)
- [x] T028 [US2] Add custom message formatting to SlackMessageBuilder (user text, optional color, custom fields)
- [x] T029 [US2] Wire SlackExtension registration in SlackFactory (register function extension)
- [ ] T030 [US2] Add concurrent message handling support (multiple processes calling slackMessage simultaneously)
- [ ] T031 [US2] Add rate limit compliance for custom messages (prevent violations when many processes send messages)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently. Users have both automatic and custom notification capabilities.

---

## Phase 5: User Story 3 - Notification Customization (Priority: P3)

**Goal**: Users can customize which events trigger notifications, message templates, and included metadata

**Independent Test**: Modify configuration to disable specific events, customize message templates, and add custom fields. Verify only configured notifications are sent with specified formatting.

### Implementation for User Story 3

- [x] T032 [P] [US3] Add notifyOnStart, notifyOnComplete, notifyOnError boolean config options to SlackConfig
- [x] T033 [P] [US3] Add startMessage, completeMessage, errorMessage configuration parsing to SlackConfig (support both String and Map formats)
- [x] T034 [US3] Add MessageTemplate entity parsing in SlackConfig (text, color, includeFields, customFields)
- [x] T035 [US3] Add custom message template application in SlackMessageBuilder (use template text instead of default)
- [x] T036 [US3] Add custom color application in SlackMessageBuilder (use template color instead of default)
- [x] T037 [US3] Add includeFields filtering in SlackMessageBuilder (only include configured fields)
- [x] T038 [US3] Add customFields merging in SlackMessageBuilder (append user custom fields to default fields)
- [x] T039 [US3] Add event filtering in SlackObserver (check notifyOnStart/Complete/Error before sending)
- [ ] T040 [US3] Add channel override support in SlackConfig and SlackClient (optional channel parameter)
- [x] T041 [US3] Add username and iconEmoji customization in SlackConfig

**Checkpoint**: All user stories should now be independently functional. Users have full control over notification behavior and formatting.

---

## Phase 6: Progressive Configuration Examples

**Purpose**: Create working example configurations demonstrating progressive feature adoption

- [x] T042 [P] Create 01-minimal.config in example/configs/ (webhook only, use defaults)
- [x] T043 [P] Create 02-notification-control.config in example/configs/ (enable/disable specific events)
- [x] T044 [P] Create 03-message-text.config in example/configs/ (simple string message customization)
- [x] T045 [P] Create 04-message-colors.config in example/configs/ (custom colors using map format)
- [x] T046 [P] Create 05-custom-fields.config in example/configs/ (add custom metadata fields)
- [x] T047 [P] Create 06-selective-fields.config in example/configs/ (choose which default fields to show)
- [x] T048 Create example/configs/README.md documenting progressive example path

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T049 [P] Update main README.md with Quick Start section from quickstart.md
- [x] T050 [P] Update main README.md with Configuration section documenting all options
- [x] T051 [P] Update main README.md with Custom Messages section and examples
- [x] T052 [P] Update main README.md with Troubleshooting section
- [x] T053 [P] Add inline Groovydoc comments to all public methods in SlackConfig
- [x] T054 [P] Add inline Groovydoc comments to all public methods in SlackClient
- [x] T055 [P] Add inline Groovydoc comments to all public methods in SlackMessageBuilder
- [x] T056 [P] Add inline Groovydoc comments to all public methods in SlackObserver and SlackExtension
- [x] T057 Verify all example configs parse correctly with SlackConfig
- [ ] T058 Run manual end-to-end test with real Slack webhook following quickstart.md
- [x] T059 Create CHANGELOG.md documenting feature additions for this release
- [x] T060 [P] Verify SlackConfigTest.groovy covers all configuration parsing scenarios (webhook validation, message templates, event flags)
- [x] T061 [P] Verify SlackClientTest.groovy covers HTTP POST, retry logic, rate limiting, and error handling
- [x] T062 [P] Verify SlackMessageBuilderTest.groovy covers Block Kit formatting for start/complete/error messages and custom fields
- [x] T063 [P] Verify SlackObserverTest.groovy covers event handling (onFlowStart/Complete/Error) and SlackExtension covers slackMessage() function

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Examples (Phase 6)**: Can start after US3 complete (needs all config options)
- **Polish (Phase 7)**: Depends on all user stories and examples being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Builds on SlackMessageBuilder from US1 but independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Extends configuration from US1 but independently testable

### Within Each User Story

**User Story 1 (P1)**:

- T012-T013 can run in parallel (SlackObserver and SlackFactory are independent files)
- T014-T016 are sequential (event handlers in same file)
- T017-T019 can run in parallel (different message formatting methods)
- T020-T022 are sequential (wiring and integration)

**User Story 2 (P2)**:

- T023 must complete first (creates SlackExtension class)
- T024-T025 can run in parallel (different function overloads)
- T026-T031 are mostly sequential (build on previous functionality)

**User Story 3 (P3)**:

- T032-T033 can run in parallel (different config sections)
- T034 must follow T033 (depends on config parsing)
- T035-T038 can run in parallel (different formatting aspects in SlackMessageBuilder)
- T039-T041 depend on earlier tasks but can run in parallel with each other

**Phase 6 (Examples)**:

- T042-T047 can ALL run in parallel (independent config files)
- T048 should run last (documents all examples)

**Phase 7 (Polish)**:

- T049-T052 can run in parallel (different README sections)
- T053-T056 can run in parallel (different source files)
- T060-T063 can run in parallel (different test files)
- T057-T059 are sequential (need prior work complete)

### Parallel Opportunities

- All Setup tasks (T001-T003) can run in parallel
- Within Foundational (T005-T006 can run in parallel)
- Within US1 (T012-T013 parallel, T017-T019 parallel)
- Within US2 (T024-T025 parallel after T023)
- Within US3 (T032-T033 parallel, T035-T038 parallel, T039-T041 parallel)
- All example configs (T042-T047) can run in parallel
- Multiple documentation tasks in Polish phase can run in parallel

---

## Parallel Example: User Story 1

```bash
# After Foundational phase completes, launch these US1 tasks in parallel:

# Parallel batch 1: Create core classes
Task T012: "Implement SlackObserver in src/main/groovy/nextflow/slack/SlackObserver.groovy"
Task T013: "Implement SlackFactory in src/main/groovy/nextflow/slack/SlackFactory.groovy"

# After T012 completes, run event handlers sequentially (same file):
Task T014: "Implement onFlowStart in SlackObserver"
Task T015: "Implement onFlowComplete in SlackObserver"
Task T016: "Implement onFlowError in SlackObserver"

# Parallel batch 2: Message formatting (can overlap with T014-T016)
Task T017: "Add workflow start message formatting to SlackMessageBuilder"
Task T018: "Add workflow completion message formatting to SlackMessageBuilder"
Task T019: "Add workflow error message formatting to SlackMessageBuilder"

# Sequential final wiring:
Task T020: "Wire SlackObserver in SlackFactory"
Task T021: "Add unavailability handling in SlackObserver"
Task T022: "Update SlackPlugin entry point"
```

---

## Parallel Example: Examples Phase

```bash
# All example configs can be created in parallel:

Task T042: "Create 01-minimal.config"
Task T043: "Create 02-notification-control.config"
Task T044: "Create 03-message-text.config"
Task T045: "Create 04-message-colors.config"
Task T046: "Create 05-custom-fields.config"
Task T047: "Create 06-selective-fields.config"

# Then create README documenting progression:
Task T048: "Create example/configs/README.md"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational (T004-T011) - CRITICAL blocking phase
3. Complete Phase 3: User Story 1 (T012-T022)
4. **STOP and VALIDATE**: Test User Story 1 independently with a real Slack webhook
5. Create minimal example config (T042) for testing
6. Deploy/demo if ready

**MVP Deliverable**: Automatic workflow notifications working end-to-end

### Incremental Delivery

1. Complete Setup + Foundational (T001-T011) → Foundation ready
2. Add User Story 1 (T012-T022) → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 (T023-T031) → Test independently → Deploy/Demo (custom messages added!)
4. Add User Story 3 (T032-T041) → Test independently → Deploy/Demo (full customization!)
5. Add Progressive Examples (T042-T048) → Documentation complete
6. Add Polish (T049-T059) → Production-ready
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T011)
2. Once Foundational is done:
   - **Developer A**: User Story 1 (T012-T022) - highest priority, MVP path
   - **Developer B**: Begin researching User Story 2, prepare tests
   - **Developer C**: Begin drafting example configurations
3. After US1 complete:
   - **Developer A**: User Story 2 (T023-T031)
   - **Developer B**: User Story 3 (T032-T041) - can work in parallel with US2
   - **Developer C**: Progressive examples (T042-T048)
4. Polish phase: Team collaborates on documentation and final validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- The existing codebase already has some implementation - verify and enhance rather than recreate from scratch
- Focus on completing working features incrementally rather than building everything at once

## Task Count Summary

- **Phase 1 (Setup)**: 3 tasks
- **Phase 2 (Foundational)**: 8 tasks ⚠️ BLOCKING
- **Phase 3 (US1 - MVP)**: 11 tasks 🎯
- **Phase 4 (US2)**: 9 tasks
- **Phase 5 (US3)**: 10 tasks
- **Phase 6 (Examples)**: 7 tasks
- **Phase 7 (Polish)**: 15 tasks (includes 4 test validation tasks)

**Total**: 63 tasks

**Parallel Opportunities**:

- Setup: 2 of 3 tasks can run in parallel
- Foundational: 2 of 8 tasks can run in parallel (T005, T006)
- US1: 5 parallelizable (T012-T013, T017-T019)
- US2: 2 parallelizable (T024-T025)
- US3: 8 parallelizable (T032-T033, T035-T038, T039-T041)
- Examples: 6 of 7 can run in parallel (all except README)
- Polish: 12 of 15 can run in parallel (T049-T056, T060-T063)

**MVP Scope**: 22 tasks (Setup + Foundational + US1)
**Full Feature**: 63 tasks across all 3 user stories (including test validation)
