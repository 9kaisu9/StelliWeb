# JSON blob for field values instead of EAV

All field values for an Entry are stored as a single JSON object in a `field_values` column, keyed by field definition ID. We chose this over an Entity-Attribute-Value (EAV) table, which is the conventional approach for dynamic schemas.

EAV would require a separate row per field value per entry, making reads expensive (one join per field), writes transactional across many rows, and sort/filter queries complex. A JSON blob keeps each Entry as a single row — reads, writes, and updates are one SQL statement. SQLite's `json_extract` function provides sufficient query capability for sort and filter operations.

The trade-off: individual field values are not first-class SQL columns and cannot be indexed conventionally. This is acceptable because the app targets a single user with modest data volumes where full-table scans are fast enough, and the `search_text` column handles the primary search case without touching the JSON.
