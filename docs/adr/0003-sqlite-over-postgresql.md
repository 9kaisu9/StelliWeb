# SQLite over PostgreSQL

We use SQLite as the database instead of PostgreSQL. The app is a locally-run, single-user tool distributed via Docker Compose. SQLite requires no separate database process — the database is a single file bind-mounted from the host at `~/stelli/stelli.db`. This eliminates the operational complexity of running a database container alongside the app and keeps the setup to a single `docker compose up`.

PostgreSQL was the original dependency in `pom.xml` and is the default assumption for Spring Boot apps. We replaced it because zero-install local deployment was a hard requirement, not a nice-to-have. SQLite's limitations (no concurrent writes, no advanced SQL features) are irrelevant for a single-user personal app.
