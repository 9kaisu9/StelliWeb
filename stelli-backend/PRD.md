# PRD: Stelli — Personal List & Memory Manager (Backend)

## Problem Statement

People who want to track and rate personal experiences — restaurants visited, travel destinations, daily memories, movies watched, or any other custom category — have no good tool that combines free-form data ownership with voice-driven entry capture. Existing tools (Notion, Airtable) are either too complex, cloud-dependent, or don't support voice-to-field entry. Users want a simple, local, self-hosted application where they can define their own lists, their own fields, and add entries by speaking naturally.

## Solution

Stelli is a locally-run, self-hosted full-stack application with a Spring Boot backend and React frontend. Users create fully custom list types (e.g. "Restaurants", "Public Toilets", "Dream Destinations") with user-defined fields of various types. They add entries to those lists manually or via voice memo — the backend transcribes audio using Whisper (locally) and extracts field values using a user-configured open-source LLM (via Ollama), returning a pre-filled entry for the user to review and save. All data lives on the user's own machine. No cloud, no subscription, no data sharing.

## User Stories

### List Management
1. As a user, I want to create a new list with a custom name, so that I can track any category of experience I care about.
2. As a user, I want to define custom fields for my list (name, type), so that each entry captures exactly the data I want.
3. As a user, I want to choose from field types including text, number, rating, date, boolean, image, video, location, single-select option, and multi-select option, so that my data is structured appropriately.
4. As a user, I want to define the available choices for option and multi-option fields, so that entries are consistent and easy to fill.
5. As a user, I want to edit an existing list's name or description, so that I can refine it over time.
6. As a user, I want to add, remove, or reorder fields on an existing list, so that my schema can evolve as my needs change.
7. As a user, I want to delete a list and all its entries, so that I can remove categories I no longer use.
8. As a user, I want to create a list from a default template (Restaurants, Movies, Journal, Travel Sites), so that I can get started quickly without defining every field from scratch.
9. As a user, I want to see all my lists in one place, so that I can navigate between them easily.

### Entry Management
10. As a user, I want to add a new entry to a list by filling in its fields, so that I can record an experience.
11. As a user, I want to edit an existing entry's field values, so that I can correct or update information.
12. As a user, I want to delete an entry from a list, so that I can remove records I no longer want.
13. As a user, I want to upload an image or video as a field value in an entry, so that I can attach visual memories to records.
14. As a user, I want to enter a geographic location as a field value, so that I can associate entries with places.
15. As a user, I want to view all entries in a list, so that I can browse my records.
16. As a user, I want to sort entries in a list by any field (ascending or descending), so that I can rank or order my records meaningfully.
17. As a user, I want to filter entries within a list by a field value, so that I can narrow down to relevant records.
18. As a user, I want text-based search within a list, so that I can quickly find a specific entry by keyword.

### Voice Entry
19. As a user, I want to record a voice memo and have it transcribed automatically, so that I can add entries hands-free.
20. As a user, I want the transcribed voice memo to be analyzed by an AI that extracts values for my list's fields, so that my entry is pre-filled without manual typing.
21. As a user, I want fields that the AI could not confidently extract to be left blank rather than guessed, so that I can fill them in manually without correcting wrong data.
22. As a user, I want to review and edit the pre-filled entry before saving it, so that I remain in control of my data.
23. As a user, I want the voice transcription and field extraction to happen entirely on my local machine, so that my data never leaves my device.
24. As a user, I want to configure which Whisper model size is used for transcription, so that I can trade off speed vs. accuracy based on my hardware.
25. As a user, I want to configure which Ollama model is used for field extraction, so that I can choose the best model available on my machine.

### Templates
26. As a user, I want a "Restaurants" template with sensible default fields (name, cuisine type, rating, price range, location, notes, has usable toilet), so that I can start tracking restaurants immediately.
27. As a user, I want a "Movies" template with sensible default fields (title, director, genre, rating, year, notes), so that I can start a movie log immediately.
28. As a user, I want a "Journal" template with sensible default fields (date, title, mood, entry text), so that I can start daily journaling immediately.
29. As a user, I want a "Travel Sites" template with sensible default fields (name, country, city, rating, visited date, notes, location), so that I can start a travel ranking list immediately.
30. As a user, I want templates to be fully editable after creation, so that they serve as starting points rather than constraints.

### Configuration & Setup
31. As a user, I want a single `.env` file to configure the application (Whisper model, Ollama URL, Ollama model, upload path, database path), so that setup is simple and transparent.
32. As a user, I want the application to start with `docker compose up`, so that I don't need to install Java or Python manually.
33. As a user, I want my data (database and uploaded files) stored in a folder on my own machine (e.g. `~/stelli/`), so that I can back it up, inspect it, or move it freely.
34. As a user, I want clear documentation in the README explaining that Ollama must be installed and running separately, so that I understand what prerequisites are needed.

### Future-Proofing (in-scope as design constraints, not features)
35. As a developer, I want a `user_id` field on lists and entries from day one, so that multi-user support can be added later without a painful migration.
36. As a developer, I want a `search_text` column on entries maintained on every save, so that cross-list full-text search can be enabled in a future version with a single SQLite FTS5 migration.

## Implementation Decisions

### Architecture
- **Spring Boot (Java 21)** as the sole backend process. No Python microservice.
- **Whisper** (OpenAI open-source STT model) is invoked as a command-line subprocess from the Voice Processing Module. Audio is written to a temp file, Whisper CLI is called, output is parsed.
- **Spring AI with Ollama** (`spring-ai-starter-model-ollama`, already in `pom.xml`) handles LLM field extraction natively from Java. No separate process needed.
- **SQLite** replaces PostgreSQL as the database. Chosen for zero-install local deployment. The SQLite file is bind-mounted from `~/stelli/stelli.db`.
- **Docker Compose** runs the Spring Boot container only. Ollama runs on the host machine and is accessed via `host.docker.internal`.
- Uploaded files (images, videos) are stored on the host filesystem at `~/stelli/uploads/`, bind-mounted into the container. Served as static resources by Spring Boot.
- No authentication or security layer in v1. Spring Security (currently in `pom.xml`) should be removed or fully disabled.

### Database Schema
- `users` table: reserved for future multi-user support. Single default user seeded on startup.
- `lists` table: id, user_id, name, description, icon, created_at, updated_at.
- `field_definitions` table: id, list_id, name, field_type (enum), options (JSON array for OPTION/MULTI_OPTION types), order_index, required, created_at.
- `entries` table: id, list_id, user_id, field_values (JSON blob), search_text (denormalized string of all text-like values), created_at, updated_at.
- No EAV. All field values stored as a single JSON object keyed by field definition ID.

### Field Types (enum)
`TEXT`, `NUMBER`, `RATING`, `DATE`, `BOOLEAN`, `IMAGE`, `VIDEO`, `LOCATION`, `OPTION`, `MULTI_OPTION`

- `IMAGE` and `VIDEO` store a relative file path string as their value.
- `LOCATION` stores a JSON object with `lat` and `lng` decimal fields.
- `OPTION` stores a single string matching one of the field's defined choices.
- `MULTI_OPTION` stores a JSON array of strings.

### Modules

**List Module**
- Service layer: create/read/update/delete list definitions and their field schemas.
- REST endpoints: `GET /api/lists`, `POST /api/lists`, `GET /api/lists/{id}`, `PUT /api/lists/{id}`, `DELETE /api/lists/{id}`, `PUT /api/lists/{id}/fields`.
- Validates that field types are valid and that OPTION/MULTI_OPTION fields include at least one choice.

**Entry Module**
- Service layer: create/read/update/delete entries; sort by any field via `json_extract`; filter by field value; within-list text search against `search_text`.
- Maintains `search_text` on every save by concatenating all text-like field values.
- REST endpoints: `GET /api/lists/{listId}/entries` (with sort/filter/search query params), `POST /api/lists/{listId}/entries`, `GET /api/lists/{listId}/entries/{id}`, `PUT /api/lists/{listId}/entries/{id}`, `DELETE /api/lists/{listId}/entries/{id}`.

**File Storage Module**
- Accepts multipart file uploads for IMAGE and VIDEO fields.
- Stores files under `${app.uploads-path}/{listId}/{entryId}/{fieldId}/{filename}`.
- Returns a relative path stored in the entry's JSON blob.
- Serves files at `/api/files/**` mapped to the uploads directory.
- REST endpoint: `POST /api/files/upload`.

**Voice Processing Module**
- Accepts a multipart audio file upload.
- Writes audio to a temp file and invokes the Whisper CLI subprocess synchronously.
- Parses Whisper JSON output to extract the transcript string.
- Sends the transcript + list field schema to the configured Ollama model via Spring AI, with a structured prompt requesting JSON field extraction.
- Returns partial fill: a map of field definition ID → extracted value (only for fields confidently extracted).
- REST endpoint: `POST /api/lists/{listId}/voice-entry`.

**Template Seeder**
- Runs on application startup (via `ApplicationRunner`) if no lists exist.
- Seeds four default list templates: Restaurants, Movies, Journal, Travel Sites — each with appropriate default field definitions.
- Idempotent: skips seeding if lists already exist.

**Configuration Module**
- All config read from `application.properties` / environment variables.
- Key properties: `app.whisper.model`, `app.ollama.base-url`, `app.ollama.model`, `app.uploads-path`, `app.db-path`.
- No runtime config API in v1 — user edits `.env` and restarts.

### API Design
- REST with JSON request/response bodies.
- Standard HTTP status codes: 200 OK, 201 Created, 204 No Content, 400 Bad Request, 404 Not Found.
- Sorting: `?sortField={fieldId}&sortDir=asc|desc`
- Filtering: `?filterField={fieldId}&filterValue={value}`
- Search: `?q={searchTerm}`

### Configuration (.env)
```
WHISPER_MODEL=base
OLLAMA_BASE_URL=http://host.docker.internal:11434
OLLAMA_MODEL=llama3
UPLOADS_PATH=/data/uploads
DB_PATH=/data/stelli.db
```
Host mounts `~/stelli/uploads` and `~/stelli/stelli.db` into `/data/` inside the container.

## Testing Decisions

**What makes a good test:** Tests verify externally observable behavior — what a module returns given specific inputs — not how it achieves the result internally. No mocking of internal collaborators; only mock external boundaries (filesystem, subprocess, Ollama HTTP calls).

### List Module Tests
- Creating a list persists it and returns it with correct fields.
- Creating a list with OPTION type and no choices returns 400.
- Adding a field to an existing list updates the schema.
- Deleting a list cascades to its field definitions and entries.
- Fetching a non-existent list returns 404.

### Entry Module Tests
- Creating an entry persists field values as a JSON blob.
- Creating an entry populates `search_text` with all text-like values.
- Fetching entries sorted by a numeric field returns them in correct order.
- Within-list text search returns only matching entries.
- Updating an entry refreshes `search_text`.
- Deleting an entry removes it from the list.

### Voice Processing Module Tests
- Given a Whisper transcript and a list schema, the extracted field map contains correctly identified values.
- Fields not mentioned in the transcript are absent from the result (partial fill).
- If Whisper subprocess fails, a descriptive error is returned.
- The Ollama prompt includes the full field schema so the model has context to extract values.

## Out of Scope

- **Frontend (React)** — this PRD covers the backend only.
- **Authentication and multi-user support** — v1 is single-user with no login. `user_id` is reserved in the schema.
- **Cross-list full-text search** — within-list search only. `search_text` column prepares for this in a future version.
- **Drag-to-reorder entries** — entries are sorted by field value, not manual position.
- **Journal-specific UI** — Journal is a standard list template; no calendar view or special date navigation.
- **Hosting or cloud deployment** — the application is local-only.
- **LLM bundling** — users must install and run Ollama separately.
- **Whisper bundling inside Docker** — Whisper CLI must be available in the Spring Boot container; model weights are downloaded on first run.

## Further Notes

- Spring Boot version is 4.0.6 and Spring AI version is 2.0.0-M5 (already in `pom.xml`). Spring AI's Ollama integration is already configured as a dependency — use `OllamaChatModel` or `ChatClient` from Spring AI rather than calling the Ollama HTTP API manually.
- The PostgreSQL driver and Spring Security dependency in `pom.xml` should be replaced/removed: swap PostgreSQL for an SQLite driver (`org.xerial:sqlite-jdbc` + `hibernate-community-dialects`) and remove or fully disable Spring Security for v1.
- Lombok is already included — use it freely for entity and DTO boilerplate.
- Whisper CLI must be installed in the Docker image. The `Dockerfile` should install Python, pip, and `openai-whisper` during the image build. Model weights should be downloaded into the Docker volume or a bind-mounted cache directory to avoid re-downloading on every container rebuild.
- The LLM extraction prompt should be carefully engineered: it must pass the list's field names, types, and option choices to the model, and instruct it to return a JSON object with only the fields it can confidently extract.
- For `RATING` fields, define a standard scale (e.g. 1–5) in the field definition and document it so both the LLM prompt and the frontend can render it consistently.
