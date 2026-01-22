# PlutoProject Agent Guidelines

## Build Commands
- Build project: `gradlew.bat build` (Windows) or `./gradlew build` (Unix)
- Build shadow JAR: `gradlew.bat shadowJar`
- Run tests: `gradlew.bat test`
- Run single test: `gradlew.bat test --tests "fully.qualified.TestCase"`
- Run checks: `gradlew.bat check`

## Code Style
- Kotlin official style (`kotlin.code.style=official` in gradle.properties)
- Indentation: 4 spaces, no tabs
- Naming: PascalCase for classes, camelCase for variables/functions
- Immutability: Prefer `val` over `var`
- Types: Explicit when unclear, inferred otherwise
- Expression bodies: Use `= body` for single-expression functions
- Error handling: Use `require`/`check` for validation, `error()` for unrecoverable errors
- Imports: Group with blank lines; wildcards allowed for internal APIs
- Line length: No strict limit; keep readable

## Conventions
- Use `override` modifier on separate line for properties
- Use `by lazy` for expensive initialization
- Prefer `when` over complex if‑else chains
- Use extension functions for utility operations
- Platform: Java 21, Kotlin 2.1.10, Paper/Velocity APIs