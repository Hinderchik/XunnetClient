# Contributing to Xunnet

Thank you for your interest in contributing to Xunnet! This document provides guidelines for participating in the project.

---

## Code of Conduct

- Be respectful and inclusive.
- Provide constructive feedback.
- Focus on what is best for the community and users.

---

## How to Contribute

### Reporting bugs

1. Check existing issues first.
2. Open a new issue with:
   - Clear title
   - Steps to reproduce
   - Expected vs actual behavior
   - Device/OS version
   - Logs (if applicable)

### Suggesting features

1. Open a GitHub Discussion or Issue.
2. Describe the use case and proposed solution.
3. Be ready to discuss implementation details.

### Pull requests

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/my-feature`.
3. Make your changes.
4. Add tests and update documentation.
5. Ensure CI passes.
6. Submit a pull request with a clear description.

---

## Development workflow

### Branch naming

- `feature/*` — new features
- `bugfix/*` — bug fixes
- `docs/*` — documentation updates
- `refactor/*` — code refactoring

### Commit messages

Use conventional commits:

```
feat: add federation sync
fix: resolve memory leak in VPN service
docs: update link format spec
test: add subscription parser tests
```

### Code style

- Kotlin: official Kotlin style guide, ktlint
- C++: LLVM/Clang format
- Go: gofmt
- TypeScript: ESLint + Prettier

---

## Testing

All contributions must include tests:

- Unit tests for business logic
- Integration tests for data layers
- UI tests for critical user flows

Run tests before submitting:

```bash
./gradlew test          # Android
ctest                   # Desktop
go test ./...           # Panel
```

---

## Security

- Never commit API keys, passwords, or signing keys.
- Report security issues privately to maintainers.
- Follow secure coding practices.

---

## License

By contributing, you agree that your contributions will be licensed under GPL-3.0.
