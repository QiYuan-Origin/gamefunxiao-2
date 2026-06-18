# Repository Guidelines

## Project Structure & Module Organization
GameFunXiao is a Java 21 Paper plugin built with Maven. Main code lives in `src/main/java/org/gamefunxiao/`, grouped by feature: `commands`, `config`, `game`, `listeners`, `menu`, `scoreboard`, `world`, and related support packages. Bukkit/Paper descriptors and default plugin assets are in `src/main/resources/`, including `plugin.yml`, `paper-plugin.yml`, `config.yml`, `messages.yml`, `config/*.yml`, and bundled flash-note data. Build output is generated under `target/`. There is currently no `src/test` tree; add tests there if introducing automated coverage.

## Build, Test, and Development Commands
- `mvn clean package` — compile Java 21 sources, process resources, and create the shaded plugin jar in `target/`.
- `mvn test` — run Maven tests if a `src/test` suite is added.
- `mvn -q clean package` — quieter build for quick validation.
Use a local Paper 1.21.x test server for runtime checks; do not commit generated server files or `target/` artifacts unless explicitly required.

## Coding Style & Naming Conventions
Use 4-space indentation and standard Java conventions: `PascalCase` classes, `camelCase` methods/fields, and lowercase package names. Keep new gameplay code inside the existing GameFun architecture instead of creating parallel systems. Reuse current menu, message, permission, data, and manager patterns before adding new abstractions. Store user-facing text in `messages.yml` or resource configs, not scattered hard-coded strings.

## Testing Guidelines
Automated tests are not established yet. When adding tests, use JUnit under `src/test/java` and name files `*Test.java`. Always run `mvn clean package` before submitting changes. For plugin features, manually verify command tab completion, permissions, menus, room creation/join/leave flows, reload behavior, and that GUI items cannot be taken unless the menu is intentionally designed for item input.

## Commit & Pull Request Guidelines
This repository currently has no commit history to infer a strict style. Use concise imperative commits, for example `Add flash mode room filtering` or `Fix menu click cancellation`. All changes must be submitted through a pull request instead of pushing directly to the protected/default branch. Before merging, perform a self-review of the full diff and verify that the PR description lists validation commands, affected config/resource files, and screenshots or short clips for menu/UI changes when applicable. Do not include tool-related signatures, labels, co-author trailers, generated-by notices, or other generated-source identifiers in commits, pull requests, code comments, documentation, or generated files.

## Configuration & Resource Notes
Default configs must be updated in `src/main/resources` when behavior changes. Keep command permissions synchronized with `plugin.yml`, and keep PlaceholderAPI/Paper compatibility in mind.
