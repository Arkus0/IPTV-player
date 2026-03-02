# IPTV Player - Project Guide

## Overview

IPTV Player application. This project is in early development stage.

### Project Type

Web Application / IPTV Streaming Player.

### Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd IPTV-player

# View available custom commands
ls .claude/commands/

# Get help with custom commands
/help-commands     # View all available commands and usage

# Use custom commands (examples)
/commit            # Create conventional commits
/custom-init       # Re-generate this CLAUDE.md with full codebase analysis
/issue <number>    # Work on GitHub issues
/reviewpr <number> # Review pull requests
/test <scope>      # Run and improve tests
```

## Architecture

### Project Structure

```
IPTV-player/
├── CLAUDE.md                       # Project guide for AI assistants
├── .gitmessage                     # Git commit message template
├── .github/                        # GitHub templates and workflows
│   ├── pull_request_template.md    # Standardized PR template
│   └── COMMIT_CONVENTION.md        # Commit best practices guide
└── .claude/                        # Claude Code configuration
    ├── commands/                   # Custom slash commands
    │   ├── commit.md               # Conventional commit helper
    │   ├── custom-init.md          # CLAUDE.md generation command
    │   ├── help-commands.md        # Command help and usage guide
    │   ├── issue.md                # GitHub issue workflow
    │   ├── reviewpr.md             # Pull request review tool
    │   └── test.md                 # Test suite management
    └── agents/                     # Specialized AI agents
        ├── general-backend-developer.md
        ├── general-code-quality-debugger.md
        ├── general-devops.md
        ├── general-frontend-developer.md
        ├── general-fullstack-developer.md
        ├── general-pm.md
        ├── general-qa.md
        ├── general-solution-architect.md
        ├── general-technical-project-lead.md
        └── general-technical-writer.md
```

## Technology Stack

### Core Technologies

- **Documentation**: Markdown.
- **Version Control**: Git.
- **Claude Code**: Custom commands, workflows, and specialized AI agents.

*(Update this section as the project stack is defined)*

### Dependencies

- **GitHub CLI (`gh`)**: Required for issue and PR management commands.

## Custom Claude Code Commands

- **Location**: `.claude/commands/`
- **Help**: Use `/help-commands` for detailed usage information.

### Available Commands

- `/custom-init` - CLAUDE.md Generator.
- `/commit` - Conventional Commits.
- `/help-commands` - Command Help and Usage Guide.
- `/issue` - GitHub Issue Workflow.
- `/reviewpr` - Pull Request Review.
- `/test` - Test Suite Management.

### Template Integration

Commands reference standardized templates:

- **Commit messages**: @.gitmessage - Four format variants (single-line, multiline, parent/child, post-review).
- **Pull requests**: @.github/pull_request_template.md - Structured PR format.
- **Commit conventions**: @.github/COMMIT_CONVENTION.md - Best practices guide.

### Agent Integration

Commands leverage specialized AI agents:

**Core Agents:**
- **general-purpose** - Complex multi-step analysis, file searching, and task coordination
- **general-solution-architect** - Architecture analysis, technology stack decisions, and design patterns
- **general-technical-writer** - Documentation creation, formatting, and content organization

**Development Agents:**
- **general-fullstack-developer** - End-to-end feature implementation spanning multiple layers
- **general-backend-developer** - API development, database patterns, and server-side logic
- **general-frontend-developer** - UI/UX implementation, component patterns, and browser automation

**Quality Assurance Agents:**
- **general-qa** - Testing strategies, automation, and comprehensive validation
- **general-code-quality-debugger** - Code review, debugging, and quality assessment
- **general-technical-project-lead** - Security assessments, strategic decisions, and architectural review

**Operations Agents:**
- **general-devops** - Infrastructure automation, CI/CD, container orchestration, and monitoring
- **general-pm** - Product management, issue creation, sprint tracking, and stakeholder communication

**Agent Usage by Command:**
- **`/custom-init`**: solution-architect, technical-writer, general-purpose
- **`/commit`**: code-quality-debugger, technical-project-lead
- **`/issue`**: fullstack-developer, backend-developer, frontend-developer, qa, general-purpose
- **`/reviewpr`**: code-quality-debugger, technical-project-lead, qa, solution-architect
- **`/test`**: qa, code-quality-debugger, backend-developer, frontend-developer

## Command Prerequisites

### GitHub CLI Setup

```bash
# Install GitHub CLI
gh --version

# Authenticate
gh auth login

# Verify access
gh repo view
```

### Git Template Configuration

```bash
# Set commit message template
git config commit.template .gitmessage

# Verify template is set
git config commit.template
```

## Notes for AI Assistants

### When working with this repository:

1. **Content Focus**: IPTV Player application + custom Claude Code commands + specialized AI agents.
2. **Primary Files**: `CLAUDE.md`, `.claude/commands/*.md`, and `.claude/agents/*.md`.
3. **Template Files**: Reference `.gitmessage` and `.github/` templates using `@` prefix.
4. **Command Usage**: Test commands in appropriate project contexts.
5. **Agent Integration**: Leverage specialized agents for domain-specific expertise.
6. **Update Patterns**: Maintain consistency between documentation, commands, templates, and agents.
7. **Version Control**: Track content, command, and agent changes.

### Command Development:

- **Format**: Use markdown with clear usage sections.
- **Structure**: Include usage, purpose, and step-by-step workflows.
- **Integration**: Ensure commands work with GitHub CLI, project tools, and specialized agents.
- **Template References**: Use `@` prefix to reference template files for Claude Code context.
- **Agent Coordination**: Leverage appropriate specialized agents for domain expertise.
