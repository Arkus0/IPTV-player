---
paths:
  - ".github/pull_request_template.md"
  - ".claude/commands/reviewpr.md"
---

# PR Conventions

## Template Structure

```markdown
## Summary
- Bullet point describing key implementation details
- Bullet point describing architectural changes

## Test plan
- [ ] All tests pass (X/X tests passing)
- [ ] Specific functionality verification
- [ ] Build verification

**Key Changes:**
- Highlight of major implementation details

**Verification:**
\`\`\`bash
dotnet build    # Expected status
dotnet test     # Expected status
\`\`\`

Closes #[issue-number]
```

## Rules

- Use bullet points in Summary section
- Include Test plan with checkboxes (use `[x]` for completed)
- Highlight Key Changes for reviewers
- Include Verification section with build/test commands and status
- Always link to original issue with "Closes #[issue-number]"
- NEVER add "Generated with Claude Code", author info, or extras
