# Contributing to CasualBans

First off, thank you for considering contributing to CasualBans! We welcome contributions of all kinds — bug fixes, new features, documentation improvements, and more.

## Code of Conduct

By participating in this project, you agree to be respectful and constructive. Harassment, trolling, and personal attacks will not be tolerated.

## Getting Started

1. **Fork** the repository on GitHub.
2. **Clone** your fork:
   ```bash
   git clone https://github.com/YOUR-USERNAME/CasualBans.git
   cd CasualBans
   ```
3. **Set up** the development environment:
   - Java 21+ JDK
   - Paper 1.21+ server for testing
4. **Create a branch** for your changes:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Workflow

### Building

```bash
./gradlew build          # Compile and run tests
./gradlew shadowJar      # Build deployable JAR with dependencies
```

### Code Style

- Use 4-space indentation (no tabs).
- Follow the existing code style in the project.
- Use Lombok annotations (`@Data`, `@Builder`, `@Getter`, etc.) where appropriate.
- Write Javadoc for public methods and classes.
- Keep methods focused and concise.

### Testing

- Test your changes on a Paper 1.21+ server before submitting.
- If adding new commands, verify permissions, usage, and edge cases.
- For storage changes, test with JSON, H2, and at least one SQL backend.

## Pull Request Process

1. **Keep PRs focused** — one feature/fix per pull request. Large changes should be broken into smaller PRs.
2. **Write a clear description** explaining what your PR does and why.
3. **Reference related issues** using `#issue-number`.
4. **Update documentation** if your change affects commands, permissions, configuration, or the API.
5. **Ensure the build passes** — run `./gradlew build` before submitting.
6. **Sign your commits** (optional but appreciated):
   ```bash
   git commit -s -m "feat: add new feature"
   ```

## Documentation

If you change commands, permissions, or configuration options, update the corresponding docs:

- `docs/commands.md` — New/changed commands
- `docs/permissions.md` — New/changed permission nodes
- `docs/configuration.md` — New config options
- `docs/api.md` — API changes
- `README.md` — If adding major features

Documentation uses Markdown. See existing docs for formatting examples.

## Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add new feature
fix: correct bug in X
docs: update installation guide
refactor: simplify Y
perf: improve storage performance
test: add tests for Z
chore: update dependencies
```

## Reporting Bugs

Open a [GitHub Issue](https://github.com/Kyssta/CasualBans/issues/new) with:

- Server version (Paper build number)
- Plugin version
- Console logs showing the error
- Steps to reproduce
- Config.yml (with sensitive data removed)
- Expected vs actual behavior

## Feature Requests

Open a [GitHub Issue](https://github.com/Kyssta/CasualBans/issues/new) with:

- A clear description of the feature
- Why it would be useful
- Any implementation ideas you have

## Questions?

Join the [Kyssta Discord](https://discord.gg/kyssta) for discussion.

Thank you for contributing! 🚀
