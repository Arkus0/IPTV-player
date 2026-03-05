---
paths:
  - ".gitmessage"
  - ".github/COMMIT_CONVENTION.md"
  - ".claude/commands/commit.md"
---

# Commit Conventions

## Format

Types: "New feature", "Fix issue", "Other"

Single-line: `<type>: (#<issue>) <name> - <description>.`
Without issue: `<type>: <description>.`
Multiline first line: `<type>: (#<issue>) <name>:`
Each bullet: `- <description>.`

Parent/child: first bullet is `- (#<child_issue>) <child_name>.`
Post-review: first bullet is `- Apply post-review fixes to <summary>.`

## Rules

- Single-line ends with `.` | Multiline first line ends with `:`
- Each bullet starts with `- ` and ends with `.`
- Present tense, imperative mood ("add feature" not "added feature")
- NEVER add "Generated with Claude Code", author info, or extras
- Atomic commits: one purpose per commit
- Split unrelated concerns into separate commits

## Examples

```
New feature: (#123) Add user authentication - implement JWT-based login system.
```

```
New feature: (#789) Implement dashboard analytics:
- Add user activity tracking components.
- Create data visualization charts.
- Integrate with reporting API.
```

```
Fix issue: (#456) Fix memory leak in data processor:
- Apply post-review fixes to buffer management optimization.
- Update error handling for edge cases.
```
