Open a pull request to prepare for a new release by updating version numbers and changelog.

Based on the repository's release workflow:

1. The release process requires:

   - Updating the version in `build.gradle`
   - Updating `CHANGELOG.md` with release notes
   - Creating a PR to the main branch

2. When the PR is merged to main, GitHub Actions will automatically:

   - Extract the version from `build.gradle`
   - Publish the plugin to Nextflow Plugin Registry
   - Create a git tag (e.g., `v0.2.1`)
   - Create a GitHub release with changelog notes

3. The workflow is idempotent - it skips if the version tag already exists.

Please follow these steps to create a release PR:

1. First, prompt the user for the new version number (following semantic versioning)
2. Create a new branch named `release/v{VERSION}`
3. Read the current version from `build.gradle` to inform the user
4. Ask the user what changes should be included in the CHANGELOG.md
5. Update the version in `build.gradle`
6. Search for and update old version references in documentation files:
   - Search for the old version pattern (e.g., `@0.2.1` or `0.2.1`) in these files:
     - `README.md`
     - `docs/*`
     - `example/configs/*`
     - Any other documentation files found with version references
   - Replace old version references with the new version
   - Report which files were updated to the user
7. Update `CHANGELOG.md` with the new version section following the Keep a Changelog format
8. Create a commit with message: `chore: release v{VERSION}`
9. Push the branch and create a PR to main with:
   - Title: `Release v{VERSION}`
   - Body including:
     - Summary of changes from CHANGELOG
     - Note that merging will trigger automated release workflow
     - Checklist: version updated, changelog updated, tests passing

The PR body should follow this template:

```markdown
## Release v{VERSION}

### Changes

{Extracted from CHANGELOG}

### Release Automation

When this PR is merged to `main`, the GitHub Actions workflow will automatically:

- ✅ Publish plugin to Nextflow Plugin Registry
- ✅ Create git tag `v{VERSION}`
- ✅ Create GitHub release with changelog notes

### Checklist

- [ ] Version updated in `build.gradle`
- [ ] Version references updated in documentation files
- [ ] CHANGELOG.md updated with release notes
- [ ] Tests passing
- [ ] Documentation updated (if needed)
```
