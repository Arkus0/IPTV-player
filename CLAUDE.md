# IPTV Player

Web Application / IPTV Streaming Player. Early development stage.

## Prerequisites

- **GitHub CLI (`gh`)**: Required for issue and PR management commands.
- **Git commit template**: Run `git config commit.template .gitmessage`

## Commands

Available at `.claude/commands/`:

- `/commit` - Conventional commits
- `/custom-init` - Regenerate this CLAUDE.md
- `/help-commands` - Command help and usage
- `/issue <number>` - GitHub issue workflow
- `/reviewpr <number>` - Pull request review
- `/test <scope>` - Test suite management

## Templates

- Commit format: `.gitmessage`
- PR template: `.github/pull_request_template.md`
- Commit conventions: `.github/COMMIT_CONVENTION.md`

## Agents

Specialized agents in `.claude/agents/` are auto-discovered by commands.

## Rules

Path-scoped rules in `.claude/rules/` load only when working with matching files.
