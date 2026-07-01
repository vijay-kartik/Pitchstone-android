# .clinerules content for the Android app project

> This is not itself a `.clinerules` file — it's content to copy into
> `.clinerules` at the root of the new Android project once it's created
> (see `ANDROID_APP_SPEC.md` in this repo for the feature spec this
> architecture applies to). Paste everything below the `---` into that file.

---

# Project rules: Transaction Extraction Android App

You are acting as a senior Android engineer on this project. Every piece of
code you write or modify must meet professional production-quality
standards — not a prototype, not a proof of concept. Prioritize correctness
and long-term maintainability over speed of typing. If a shortcut would
violate the principles below, do not take it without flagging the tradeoff
explicitly.

## Tech stack — mandated, not optional

These are fixed choices for this project. Do not substitute alternatives
(no Dagger-only setup instead of Hilt, no XML View system instead of
Compose, no third-party nav libraries instead of Navigation Compose) without
explicitly flagging the deviation and why.

- **Dependency injection: Hilt.** Every ViewModel, Repository implementation,
  UseCase, and Retrofit/OkHttp instance is provided via Hilt
  (`@HiltAndroidApp`, `@AndroidEntryPoint`/`hiltViewModel()`, `@Module` +
  `@InstallIn` for bindings). No manual singletons, no hand-rolled service
  locators, no constructing dependencies inline inside a class that needs
  them.
- **UI: Jetpack Compose**, exclusively — no XML layouts, no `Fragment`-based
  Views, no View-based interop unless there's a specific, justified reason
  (e.g. a library with no Compose equivalent), which must be called out
  explicitly if it comes up.
- **Navigation: Jetpack Navigation Compose** (`androidx.navigation:navigation-compose`),
  single `NavHost`/`NavController` pattern. Prefer type-safe navigation
  routes (serializable route objects, per current Navigation Compose
  conventions) over raw string routes with manually concatenated arguments.

## Dependency management & version compatibility — check before adding or upgrading anything

Getting this wrong is one of the most common sources of broken Android
builds, so treat it as a first-class concern, not an afterthought:

- **Use a Gradle version catalog** (`gradle/libs.versions.toml`) as the single
  source of truth for all dependency versions — don't scatter version
  strings across module `build.gradle.kts` files.
- **Compose libraries must be aligned via the Compose BOM**
  (`androidx.compose:compose-bom`) rather than pinning individual Compose
  artifact versions by hand — the BOM guarantees mutually-compatible
  versions across `compose-ui`, `compose-material3`, `compose-foundation`,
  etc.
- **Verify Kotlin ↔ Compose Compiler ↔ AGP compatibility explicitly** before
  picking versions: with Kotlin 2.0+, the Compose Compiler is a separate
  Gradle plugin (`org.jetbrains.kotlin.plugin.compose`) whose version must
  track the Kotlin version in use, not the old `kotlinCompilerExtensionVersion`
  scheme. Check the official Compose-Kotlin compatibility map before
  upgrading either.
- **Verify Hilt ↔ Kotlin/KSP compatibility explicitly.** Hilt's Gradle plugin
  and `hilt-compiler` version must be compatible with the project's Kotlin
  version and whether KSP or kapt is used for annotation processing (prefer
  KSP over kapt for build performance if the Hilt version in use supports
  it).
- **Verify Navigation Compose's minimum Kotlin/Compose BOM requirements**
  before adopting type-safe routes (this feature has a minimum version
  floor) — don't assume the latest syntax from documentation works with
  whatever version ends up pinned; check the changelog for the version
  actually selected.
- **Prefer stable releases** over alpha/beta/RC versions for anything this
  project depends on for production behavior, unless a specific required
  feature only exists in a pre-release version — if that trade-off is ever
  made, state it explicitly in a comment near the version catalog entry,
  including what stable version to migrate to once available.
- **Before adding a new dependency or bumping an existing one**, check for
  transitive dependency conflicts (e.g. via `./gradlew :app:dependencies` or
  Gradle's dependency insight report) rather than assuming Gradle's default
  conflict resolution picked the intended version.
- When in doubt about whether a version combination is known-good, say so
  explicitly rather than silently picking versions and hoping the build
  succeeds — a broken build from an unverified version bump is worse than
  pausing to confirm compatibility first.

## Architecture: Clean Architecture + MVVM — non-negotiable

Every feature is organized into three layers, with dependencies pointing
inward only:

```
Presentation  →  Domain  ←  Data
 (Compose UI,     (UseCases,    (Repository impls,
  ViewModels,      domain        remote/local data
  UI State)        models,       sources, DTOs,
                   repository    mappers)
                   interfaces)
```

- **Domain layer is pure Kotlin.** No `android.*` imports, no framework
  dependencies, no Retrofit/Room annotations. It defines repository
  interfaces (abstractions) and UseCases (one class = one business
  operation, e.g. `ExtractTransactionUseCase`, `SaveTransactionUseCase`,
  `GetTransactionsUseCase`, `GetTransactionByIdUseCase`). UseCases are the
  only thing ViewModels call — never call a repository directly from a
  ViewModel if a UseCase should mediate it.
- **Data layer implements domain interfaces.** Repository implementations
  here depend on remote data sources (Retrofit service interfaces) and,
  if added, local data sources (Room). DTOs (network/DB models) are mapped
  to domain models via dedicated mapper classes/functions — domain models
  never leak network annotations (`@SerializedName` etc.) and DTOs never
  leak into the domain or presentation layers.
- **Presentation layer is MVVM.** Each screen has one ViewModel exposing a
  single `StateFlow<UiState>` (a sealed interface/class with explicit
  `Loading`, `Success(data)`, `Error(message)` variants — no nullable
  "maybe loading, maybe not" boolean soup). Composables are **stateless
  where possible**: they render a `UiState`, forward user events up to the
  ViewModel via lambdas, and contain zero business logic. Enforce
  **unidirectional data flow**: events go up, state comes down — never have
  a Composable mutate ViewModel state directly.
- Dependency direction is enforced via constructor injection (Hilt) using
  interfaces defined in the domain layer — the presentation and data
  layers depend on domain abstractions, domain depends on nothing outward.
  This is the Dependency Inversion Principle applied structurally, not just
  as a naming convention.

## SOLID — apply deliberately, explain the choice when it's not obvious

- **Single Responsibility:** One class, one reason to change. A ViewModel
  prepares UI state and nothing else. A Repository orchestrates data access
  and nothing else (no business rules). A UseCase does exactly one business
  operation. If a class is doing two things, split it.
- **Open/Closed:** Prefer designs where adding a new case doesn't require
  editing existing, tested code — e.g. model JSON-extraction strategies
  (see `ANDROID_APP_SPEC.md` §5) as a small set of interchangeable
  `JsonExtractionStrategy` implementations tried in order, so adding a new
  fallback strategy later doesn't touch the existing ones.
- **Liskov Substitution:** Any implementation of a repository/data-source
  interface must be fully substitutable for another without surprising
  callers — e.g. a fake `TransactionRepository` used in tests must satisfy
  the exact same contract (including error behavior) as the real
  Retrofit-backed one.
- **Interface Segregation:** Don't create one giant `ApiService` with every
  endpoint crammed together if the consumers don't need all of it. Group by
  actual usage (e.g. an `AgentApi` interface for `/agent`, a
  `TransactionsApi` interface for `/transactions*`) rather than one
  monolithic interface every class must depend on regardless of what it
  actually calls.
- **Dependency Inversion:** ViewModels and UseCases depend on repository
  *interfaces* declared in the domain layer, never on concrete
  Retrofit/Room-backed classes directly. Wire concrete implementations via
  Hilt modules, not manual construction or service locators.

## Design patterns — use where they earn their keep, not as decoration

Actively look for and apply a suitable pattern when it genuinely improves
clarity, testability, or extensibility for this codebase. Do **not** apply a
pattern just to look sophisticated — an unnecessary abstraction layer is
itself a code smell. Patterns directly relevant to this app:

- **Repository pattern** — abstracts the data layer (network + optional
  local cache) behind a domain-facing interface. Already mandated by the
  architecture above.
- **UseCase / Interactor pattern** — one class per business operation,
  callable and composable, easy to unit test in isolation.
- **Strategy pattern** — for the multi-step JSON extraction fallback logic
  (direct parse → fenced code block → brace-matching fallback, per
  `ANDROID_APP_SPEC.md` §5): model each step as an interchangeable strategy
  rather than one long if/else chain in a ViewModel.
- **Chain of Responsibility** — an alternative/complementary framing for the
  same JSON-extraction fallback sequence, if it reads more naturally that
  way once implemented; pick one framing and be consistent, don't mix both
  for the same logic.
- **Adapter/Mapper pattern** — explicit mapper classes or extension
  functions (`fun TransactionDto.toDomain(): Transaction`) between network
  DTOs, domain models, and any UI-specific presentation models. Never reuse
  a DTO as a domain model or a UI model directly.
- **Observer pattern** — inherent to MVVM via `StateFlow`/`Flow`; this is
  how ViewModels push state to the UI. Don't reintroduce callback-based
  patterns or `LiveData` alongside `StateFlow` — pick `StateFlow`/`Flow`
  consistently.
- **Builder pattern** — already provided by the libraries in use
  (`Retrofit.Builder`, `OkHttpClient.Builder`); construct these once via a
  Hilt `@Provides` module, don't rebuild them ad hoc per call site.
- **Sealed class / Result wrapper pattern** — represent success/failure
  explicitly (e.g. a `Result<T>`/`Either`-style wrapper or a sealed
  `AppError` hierarchy) rather than throwing raw exceptions across layer
  boundaries. Map network/DB exceptions into domain-specific error types at
  the data layer boundary; the domain and presentation layers should never
  need to catch a raw `IOException` or Retrofit `HttpException` directly.
- Avoid the manual **Singleton** anti-pattern (static instances,
  companion-object-held mutable state) — let Hilt manage instance lifecycles
  and scoping instead.

## Clean code conventions

- **Naming:** intention-revealing names for classes, functions, and
  variables. No abbreviations that aren't immediately obvious. A function
  name should make a comment describing what it does unnecessary.
- **Function size and focus:** small functions doing one thing at one level
  of abstraction. If a function needs a comment to explain a block inside
  it, that block is probably a function that should be extracted and named.
- **Comments:** only for *why*, never for *what* — the code should already
  make "what" obvious through naming and structure. No commented-out code
  left in place.
- **Immutability by default:** prefer `val` over `var`, immutable data
  classes, and immutable collections at API boundaries. Mutable state should
  be contained and obvious (e.g. inside a ViewModel's private
  `MutableStateFlow`, never exposed as mutable to callers).
- **Null-safety:** use Kotlin's null-safety properly — no `!!` outside of
  genuinely provable-safe, well-commented exceptions. Prefer safe calls,
  `?:`, and sealed types that make "absence" an explicit state rather than
  a nullable field threaded everywhere.
- **No magic numbers/strings:** extract named constants (e.g. timeout
  values, file size limits, API paths) — especially important here since
  the gateway API has specific documented limits and timeouts
  (`ANDROID_APP_SPEC.md` §3.2) that must be named constants, not inline
  literals scattered across the networking code.
- **DRY, but don't over-abstract prematurely:** three similar lines are
  fine; don't build a generic framework for a case used once. Extract
  shared logic only once real duplication (not just superficial similarity)
  emerges.
- **Consistent formatting/static analysis:** follow standard Kotlin style
  conventions; if `ktlint` and/or `detekt` are configured in the project,
  code must pass both before being considered done.
- **Errors are values, not surprises:** don't let exceptions propagate
  silently or get swallowed. Every catch should either handle the error
  meaningfully (map to a domain error / UI error state) or not exist —
  never an empty catch block.

## Testability is a design constraint, not an afterthought

Because business logic lives in framework-free UseCases and Domain models,
and ViewModels depend only on interfaces, unit tests for UseCases and
ViewModels should never need Robolectric, an emulator, or a real network —
only fakes/mocks of the repository interfaces. If you find yourself needing
Android framework classes to unit test business logic, the layering has
leaked and needs to be fixed, not worked around with a heavier test setup.

## Reference

The concrete feature requirements, API contracts, endpoints, timeouts, and
open product questions this architecture is being applied to are fully
specified in `ANDROID_APP_SPEC.md` in the gateway project repo — read it
before starting implementation; don't re-derive the API shape from
assumptions.
