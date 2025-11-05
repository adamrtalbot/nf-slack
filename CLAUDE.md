# nf-slack Development Guidelines

This project uses [spec-kit](https://github.com/github/spec-kit) for spec-driven development. All development workflows follow the spec-kit methodology.

Last updated: 2025-11-05

## Core Principles & Governance

**See**: `.specify/memory/constitution.md`

The nf-slack plugin is governed by our constitution which defines:

- **Core Principles**: User-First Design, Configuration Flexibility, Test Coverage for Public APIs, Documentation as Code
- **Quality Standards**: Security, Performance, Reliability, Compatibility requirements
- **Development Workflow**: Specification → Implementation → Validation phases
- **Governance**: Amendment procedures, versioning policy, compliance reviews

All contributions MUST comply with these constitutional principles.

## Development Workflow (spec-kit)

Follow the spec-kit workflow for all feature development:

1. **Constitution** (`/speckit.constitution`) - Project principles and governance (already established)
2. **Specify** (`/speckit.specify`) - Define feature requirements and user scenarios
3. **Clarify** (`/speckit.clarify`) - Identify and resolve underspecified areas
4. **Plan** (`/speckit.plan`) - Create technical architecture and design
5. **Tasks** (`/speckit.tasks`) - Generate actionable, dependency-ordered tasks
6. **Implement** (`/speckit.implement`) - Execute the implementation plan
7. **Analyze** (`/speckit.analyze`) - Cross-artifact consistency validation

### Templates

All spec-kit templates are in `.specify/templates/`:

- `spec-template.md` - Feature specifications with user scenarios
- `plan-template.md` - Technical planning and architecture
- `tasks-template.md` - Actionable task breakdown
- `checklist-template.md` - Custom validation checklists
- `agent-file-template.md` - Agent context files

## Technology Stack

### Core Technologies

- **Language**: Groovy 3.x (Nextflow plugin standard)
- **Runtime**: Java 11+
- **Framework**: Nextflow plugin API
- **HTTP Client**: Groovy HTTP client
- **Testing**: Spock framework

### Build System

```bash
./gradlew test          # Run tests
./gradlew assemble      # Build plugin
./gradlew clean assemble -x test  # Build without tests
```

## Project Structure

```text
.specify/               # Spec-kit configuration and templates
  memory/              # Constitution and project memory
  templates/           # Feature templates (spec, plan, tasks)
  scripts/             # Workflow automation scripts
src/                   # Plugin source code
  main/groovy/         # Groovy implementation
  resources/           # Plugin resources
plugins/               # Plugin test configuration
  nf-slack/            # Plugin module
tests/                 # Spock tests
example/               # Example configurations
```

## Code Style

Follow standard Groovy conventions for Nextflow plugins:

- Use Groovy idioms and best practices
- Follow Nextflow plugin API patterns
- Maintain compatibility with Java 11+
- See constitution for specific requirements (security, performance, documentation)

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
