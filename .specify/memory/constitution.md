<!--
Sync Impact Report:
- Version change: Initial constitution → 1.0.0
- Modified principles: N/A (initial version)
- Added sections: Core Principles (4 principles), Quality Standards, Development Workflow, Governance
- Removed sections: N/A
- Templates requiring updates:
  ✅ spec-template.md - Reviewed, no updates needed (already follows constitution principles)
  ✅ plan-template.md - Reviewed, no updates needed (Constitution Check section already present)
  ✅ tasks-template.md - Reviewed, no updates needed (follows test-optional and incremental delivery principles)
- Follow-up TODOs: None
-->

# nf-slack Plugin Constitution

## Core Principles

### I. User-First Design

The nf-slack plugin MUST prioritize user experience and ease of adoption above technical elegance. Every feature MUST:
- Work with minimal configuration (webhook URL only for basic usage)
- Provide progressive disclosure of advanced features
- Include clear, actionable documentation with working examples
- Default to sensible behaviors that work for most use cases

**Rationale**: Nextflow plugins are infrastructure tools that should reduce friction, not create it. Users should get value immediately without reading extensive documentation, with advanced features discoverable as needs grow.

### II. Configuration Flexibility

The plugin MUST support multiple configuration patterns:
- Simple string-based configuration for basic usage
- Map-based configuration for advanced customization
- Progressive configuration examples from minimal to complex
- Backward compatibility when adding new features

**Rationale**: Different users have different needs. Research users need quick notifications, production pipelines need rich context and custom fields. Both should be first-class citizens.

### III. Test Coverage for Public APIs

All public-facing functionality (configuration options, functions, and event handlers) MUST have:
- Unit tests verifying behavior
- Integration tests for Slack API interactions
- Example configurations demonstrating usage
- Tests SHOULD be written before implementation (TDD encouraged but not mandated)

**Rationale**: As a plugin that integrates with external services, reliability is critical. Public APIs are contracts with users and must be verified to prevent regressions.

### IV. Documentation as Code

Every feature MUST include:
- Working example configuration files
- Progressive examples showing feature adoption path
- Inline code documentation for public APIs
- README updates explaining the feature's value

Documentation MUST be maintained in sync with code changes.

**Rationale**: Nextflow users are scientists and engineers, not plugin developers. Documentation must show them how to solve their problems, not just list API signatures.

## Quality Standards

### Security

- Webhook URLs MUST support configuration via Nextflow secrets
- No sensitive data (credentials, tokens) in logs or error messages
- HTTPS MUST be enforced for all Slack communication
- Rate limiting MUST be implemented to prevent abuse

### Performance

- Message sending MUST be asynchronous and non-blocking where possible
- Plugin initialization MUST complete in < 1 second
- Message formatting MUST not significantly delay workflow events
- Resource usage MUST be negligible compared to workflow overhead

### Reliability

- Clear error messages when Slack communication fails
- Retry logic with exponential backoff for transient failures
- Detailed logging for debugging integration issues
- Validation of webhook URLs and configuration at startup

### Compatibility

- Support Nextflow versions as per plugin compatibility matrix
- Maintain backward compatibility for existing configurations
- Deprecation notices MUST provide migration path and timeline
- Breaking changes require MAJOR version bump per SemVer

## Development Workflow

### Feature Development

1. **Specification Phase**: Define user scenarios and acceptance criteria in `.specify/specs/`
2. **Implementation Phase**:
   - Write tests for new public APIs
   - Implement feature following principles
   - Add progressive configuration examples
   - Update README with feature documentation
3. **Validation Phase**:
   - Verify all tests pass
   - Test with real Nextflow workflows
   - Validate examples work as documented

### Code Review Requirements

- All changes require review before merge
- Reviewers MUST verify:
  - Constitution compliance (especially Principles I-IV)
  - Test coverage for public APIs
  - Documentation completeness
  - Backward compatibility maintenance
- Configuration changes MUST include example files

### Quality Gates

Before release:
- All tests pass (`make test`)
- Build succeeds (`make assemble`)
- Example configurations validated with real Nextflow workflows
- README examples tested
- CHANGELOG updated with user-facing changes

## Governance

### Amendment Procedure

Constitution amendments require:
1. Documented rationale for the change
2. Impact assessment on existing principles
3. Review by plugin maintainers
4. Update of dependent templates and documentation
5. Version bump following semantic versioning:
   - MAJOR: Backward incompatible principle changes or removals
   - MINOR: New principles or materially expanded guidance
   - PATCH: Clarifications, wording fixes, non-semantic refinements

### Versioning Policy

This constitution follows semantic versioning:
- **MAJOR.MINOR.PATCH** format
- MAJOR = Breaking principle changes affecting development approach
- MINOR = New principles or significant additions
- PATCH = Clarifications and refinements

### Compliance Review

- All pull requests MUST be evaluated against Core Principles
- Violations require explicit justification and maintainer approval
- Template files in `.specify/templates/` MUST align with constitution
- Review constitution quarterly for relevance and updates

### Complexity Justification

If a feature violates constitution principles (especially Principle I: User-First Design or Principle IV: Documentation as Code):
- Document the violation in PR description
- Provide rationale for why it's necessary
- Propose mitigation strategies
- Require approval from two maintainers

**Version**: 1.0.0 | **Ratified**: 2025-10-30 | **Last Amended**: 2025-10-30
