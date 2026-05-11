# ArxivLens

ArxivLens is an Android app for discovering, saving, and exporting academic papers from arXiv. It is designed for students and researchers who want a lightweight mobile workflow for finding papers, organizing them into collections, and generating BibTeX citations for reports, papers, and LaTeX projects.

## Big Idea

"Using Jetpack Compose and Kotlin coroutines, we aim to build an Android research companion that lets users search arXiv papers by topic or venue, organize findings into named offline collections, and export them as BibTeX for direct use in LaTeX tools like Overleaf — replacing scattered browser tabs with a focused, portable, citation-ready research library."

## Problem

Students often search for research papers across several tools, save links manually, and later rebuild citations by hand. That process is slow, repetitive, and error-prone, especially when working on literature reviews or class projects.

ArxivLens makes that workflow more direct:

- Search arXiv from the app.
- Read paper details and abstracts.
- Save relevant papers into named collections.
- Export saved papers as a `.bib` file.
- Share the exported BibTeX through Android's share sheet.

## Features

- arXiv paper search using the public arXiv API (no authentication required).
- XML parsing from arXiv Atom feeds into immutable Kotlin models.
- Search result sorting by relevance, newest, oldest, revision count, DOI presence, and title.
- Paper detail screen with title, authors, categories, abstract, arXiv link, and PDF link.
- Local collections for organizing saved papers.
- Create a new collection directly while saving a paper.
- Room database persistence for collections and saved papers.
- Duplicate prevention inside a collection through a composite primary key.
- Saved papers can be reopened from collection detail.
- BibTeX export with peer-reviewed (DOI) entries ordered first, and duplicate cite-key deduplication.
- Android share sheet integration through FileProvider.
- Material 3 Jetpack Compose UI.

## Functional Programming Concepts

This project demonstrates the following Kotlin functional programming features:

| # | Concept | How it is applied | Key file(s) |
|---|---|---|---|
| 1 | **Immutable data classes** | `Paper` is a pure `data class` — no Room annotations, no mutation, structural equality via `==` | [`data/model/Paper.kt`](app/src/main/java/com/arxivlens/data/model/Paper.kt) |
| 2 | **Data class features** | `toString()` overridden for readable debug output; `copy()` used to derive PDF URL without mutation; `==` used as equality guard in ViewModel | [`data/model/Paper.kt`](app/src/main/java/com/arxivlens/data/model/Paper.kt), [`data/network/ArxivXmlParser.kt`](app/src/main/java/com/arxivlens/data/network/ArxivXmlParser.kt), [`ui/viewmodel/SearchViewModel.kt`](app/src/main/java/com/arxivlens/ui/viewmodel/SearchViewModel.kt) |
| 3 | **Sealed UI states** | `SearchUiState`, `CollectionsUiState`, `CollectionDetailUiState` — "make illegal states unrepresentable"; UI exhaustively matches with `when` | [`data/model/UiState.kt`](app/src/main/java/com/arxivlens/data/model/UiState.kt) |
| 4 | **Lambda expressions** | Passed as arguments: `map { }`, `filter { }`, `items(key = { })`, `sortedByDescending { }`. Returned from functions: `SortOption.comparator()` returns `Comparator<Paper>?` | [`data/model/SortOption.kt`](app/src/main/java/com/arxivlens/data/model/SortOption.kt), [`util/FpUtils.kt`](app/src/main/java/com/arxivlens/util/FpUtils.kt) |
| 5 | **Generics** | Custom generic extension functions: `topNBy<T, K>`, `splitBy<T>`, `frequencyMap<T>` | [`util/FpUtils.kt`](app/src/main/java/com/arxivlens/util/FpUtils.kt) |
| 6 | **Generic constraints** | `fun <T, K : Comparable<K>> List<T>.topNBy(...)` — upper-bound constraint guarantees the key type supports ordering | [`util/FpUtils.kt`](app/src/main/java/com/arxivlens/util/FpUtils.kt) |
| 7 | **Control flow** | `when` expressions in the XML parser and all Compose screens; `for` loop inside `frequencyMap`; `if/else` throughout | [`util/FpUtils.kt`](app/src/main/java/com/arxivlens/util/FpUtils.kt), [`data/network/ArxivXmlParser.kt`](app/src/main/java/com/arxivlens/data/network/ArxivXmlParser.kt) |
| 8 | **Safe calls & Elvis** | `?.trim() ?: ""`, `response.body?.string()`, `totals[key] ?: 0`, `authors.firstOrNull()?.substringAfterLast(" ")` | Throughout |
| 9 | **Kotlin Result type** | `ArxivApiService.search()` returns `Result<List<Paper>>`; folded in ViewModel with `result.fold(onSuccess = ..., onFailure = ...)` | [`data/network/ArxivApiService.kt`](app/src/main/java/com/arxivlens/data/network/ArxivApiService.kt), [`ui/viewmodel/SearchViewModel.kt`](app/src/main/java/com/arxivlens/ui/viewmodel/SearchViewModel.kt) |
| 10 | **Exception handling** | `try/catch` in `ArxivApiService`; `.catch { e -> emit(...) }` on Room Flows in ViewModels | [`data/network/ArxivApiService.kt`](app/src/main/java/com/arxivlens/data/network/ArxivApiService.kt), [`ui/viewmodel/CollectionDetailViewModel.kt`](app/src/main/java/com/arxivlens/ui/viewmodel/CollectionDetailViewModel.kt) |
| 11 | **Reactive streams** | Room `Flow` → `map` / `combine` / `catch` → `StateFlow`; `displayState` is derived reactively from `_uiState` and `_sortOption` via `combine` | [`ui/viewmodel/SearchViewModel.kt`](app/src/main/java/com/arxivlens/ui/viewmodel/SearchViewModel.kt) |
| 12 | **Collection pipelines** | `splitBy` + `zip` + `map` + `frequencyMap` + `joinToString` in BibTeX export; `groupingBy` + `eachCount` replaced by custom `frequencyMap` | [`util/BibTexExporter.kt`](app/src/main/java/com/arxivlens/util/BibTexExporter.kt) |
| 13 | **Pure / impure separation** | `BibTexExporter` (zero Android imports, zero I/O) vs `BibTexFileWriter` (Android FileProvider, I/O only) | [`util/BibTexExporter.kt`](app/src/main/java/com/arxivlens/util/BibTexExporter.kt), [`util/BibTexFileWriter.kt`](app/src/main/java/com/arxivlens/util/BibTexFileWriter.kt) |
| 14 | **MVVM pattern** | ViewModel holds all state and actions; Repository is the single data-layer interface; Compose screens only observe `StateFlow` | All `ui/viewmodel/` and `data/db/` files |

## Tech Stack

- Kotlin
- Android
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel
- Kotlin Coroutines
- Flow and StateFlow
- Room
- KSP
- OkHttp
- Android FileProvider

## Architecture

The app is organized into clear layers:

```
data/model      → immutable domain models, Room entities, sealed UI states, pure mappers
data/network    → arXiv API client (OkHttp) and pure Atom XML parser
data/db         → Room database, DAOs, repository (single data-layer interface)
ui/screens      → Compose screens — observe StateFlow, never touch the DB directly
ui/viewmodel    → state machines: StateFlow, coroutines, business logic
ui/navigation   → NavController graph and bottom navigation
util            → BibTexExporter (pure), BibTexFileWriter (Android I/O), FpUtils (generic FP utilities)
```

## Demo Flow

1. Open the app.
2. Search for a topic such as `attention mechanism` or `graph neural networks`.
3. Sort or inspect the search results (6 sort modes available).
4. Open a paper detail screen.
5. View the abstract and open the arXiv or PDF link.
6. Save a paper into an existing collection or create a new collection from the save sheet.
7. Open the collection and confirm the saved paper appears.
8. Reopen the saved paper from the saved list.
9. Export the collection as BibTeX (peer-reviewed entries appear first).
10. Share or save the `.bib` file through Android's share sheet.

## Build Instructions

1. Open this project in Android Studio.
2. Let Gradle sync finish.
3. Select an emulator or Android device (Android 8.0 / API 26 or later).
4. Run the `app` configuration.

Command-line build:

```bash
./gradlew :app:assembleDebug
```

Run local unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

## Dependencies

| Library | Purpose |
|---|---|
| Jetpack Compose + Material 3 | Declarative UI |
| Navigation Compose | Screen routing and back-stack |
| ViewModel + Lifecycle | State survival across recompositions |
| Room + KSP | Local SQLite database with generated DAOs |
| Kotlin Coroutines + Flow | Async work and reactive data streams |
| OkHttp | HTTP client for the arXiv API |
| Android FileProvider | Secure file sharing via content:// URIs |

## External Tools and Attribution

### AI Assistance

Claude (Anthropic) was used during development for:

- **Orchestration**: Suggesting project structure, package layout, and layer boundaries at the planning stage.
- **Boilerplate scaffolding**: Initial Room entity annotations, Gradle dependency version lookups, and Compose scaffold templates were generated as starting points and then reviewed, adapted, and integrated by hand.
- **Prototyping and trial**: Exploring alternative approaches — for example, evaluating TypeConverter vs. denormalized comma-separated fields for authors/categories before committing to the denormalized design.

All functional programming design decisions, domain logic (XML parsing pipeline, BibTeX generation and cite-key deduplication, `SortOption` comparator design, reactive `combine` usage), MVVM architecture choices, and the final implementation were authored, reviewed, and tested by the developer. The project goes substantially beyond any generated scaffold: it implements a complete XML parsing pipeline, a pure BibTeX generation pipeline with duplicate-key resolution, custom generic utility functions, and reactive state management with coroutines and Flow.

No external video tutorials or course project walkthroughs were followed.

## Current Limitations

- The app requires an internet connection for arXiv search.
- Saved papers are stored locally on the device only (no cross-device sync).
- There is no account system or cloud backup.
- Advanced search filters (author, category, date range) are not yet implemented.
- UI and unit testing coverage can be expanded.

## Future Improvements

- Add advanced search filters for author, title, category, and date range.
- Add offline reading: cache full abstracts locally.
- Add reading status and personal notes per saved paper.
- Add focused unit tests for `ArxivXmlParser` and `BibTexExporter` (both are pure functions, trivially testable without Android).
- Add collection-level search and sorting.
- Explore Firebase sync for cross-device collections.

## Project Summary

ArxivLens combines a real academic data source with Kotlin functional programming techniques. Raw arXiv Atom XML is parsed by a pure function into immutable `Paper` values. Those values flow through reactive UI state managed by `combine` and `StateFlow`. Saved papers can be transformed — via a pure pipeline using `splitBy`, `frequencyMap`, `zip`, `map`, and `joinToString` — into citation-ready BibTeX with automatic cite-key deduplication and peer-reviewed ordering.
