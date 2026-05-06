# Ubiquitous Language

## Core data model

| Term | Definition | Aliases to avoid |
|---|---|---|
| **List** | A user-defined collection with a named schema of typed fields | Category, list type, database, table |
| **Field** | A named, typed column in a list's schema | Column, attribute, property, field definition |
| **Entry** | A single record in a list, holding one value per field | Item, record, row, submission |
| **Field Value** | The actual data stored for one field within one entry | Value, cell, data |
| **Schema** | The complete ordered set of fields belonging to a list | Structure, template (when referring to an existing list's shape) |
| **Template** | A predefined list with default fields that a user can start from and customise | Preset, example, default |

## Fields and types

| Term | Definition | Aliases to avoid |
|---|---|---|
| **Field Type** | The kind of data a field holds; one of TEXT, NUMBER, RATING, DATE, BOOLEAN, IMAGE, VIDEO, LOCATION, OPTION, MULTI_OPTION | Data type, column type |
| **Rating** | A numeric score on a fixed 1–5 scale, used as a field type | Score, stars |
| **Option** | A single allowed string value chosen from a field's predefined choices, used as a field type | Dropdown, single select |
| **Multi-Option** | One or more allowed string values chosen from a field's predefined choices, used as a field type | Multiselect, tags, checkboxes |
| **Choice** | One allowed string value in the predefined set for an OPTION or MULTI_OPTION field | Option value, pick, selection |

## Voice entry

| Term | Definition | Aliases to avoid |
|---|---|---|
| **Voice Memo** | An audio recording submitted by the user to create an entry | Audio clip, recording, voice note |
| **Transcript** | The text produced by speech-to-text processing of a voice memo | Transcription, text output |
| **Field Extraction** | The AI process of mapping transcript content to field values for a specific list | Parsing, inference, NLP processing |
| **Partial Fill** | The pre-populated set of field values returned after field extraction, containing only confidently identified values | Pre-fill, suggestion, draft |

## People

| Term | Definition | Aliases to avoid |
|---|---|---|
| **User** | The person who owns and manages lists and entries on a local Stelli instance | Person, owner, account, customer |

## Relationships

- A **List** has exactly one **Schema** (its ordered set of **Fields**)
- A **Field** belongs to exactly one **List** and has exactly one **Field Type**
- An **OPTION** or **MULTI_OPTION** **Field** has one or more **Choices**
- An **Entry** belongs to exactly one **List** and holds one **Field Value** per **Field**
- A **Voice Memo** is processed into a **Transcript**, which is then subjected to **Field Extraction** to produce a **Partial Fill**
- A **Template** is a **List** that was seeded by the system; once created it is indistinguishable from a user-created **List**

## Example dialogue

> **Dev:** "When a **User** submits a **Voice Memo** for the Restaurants **List**, does the system immediately create an **Entry**?"

> **Domain expert:** "No — the backend returns a **Partial Fill**: a set of **Field Values** the AI confidently extracted. The **User** reviews it and decides to save. Only then is the **Entry** created."

> **Dev:** "What if the **Transcript** didn't mention the Cuisine **Field** at all?"

> **Domain expert:** "That **Field Value** is simply absent from the **Partial Fill**. The **User** fills it in manually. We never guess."

> **Dev:** "And if someone creates a new **List** — say, Public Toilets — with a Location **Field** and a Cleanliness **Rating** **Field**, that's just a normal **List**, not a **Template**?"

> **Domain expert:** "Exactly. A **Template** is only a **List** that was seeded by the system on first boot. Once it exists, it behaves identically to any other **List** the **User** created from scratch."

## Flagged ambiguities

- **"list type"** appeared occasionally to mean **List** — avoid it. A **List** already implies a specific type defined by its **Schema**; "list type" suggests a meta-category that does not exist in the domain.
- **"template"** is ambiguous: in the PRD it refers both to the seeded default **Lists** (Restaurants, Movies, etc.) and to the general concept of a starting-point schema. The canonical meaning is the former — a **Template** is a system-seeded **List**. Do not use "template" to describe a user-defined starting point; say the **User** copied or customised a **List** instead.
- **"voice entry"** appeared in the API name (`/voice-entry`) but in domain language the user submits a **Voice Memo** and receives a **Partial Fill**. The term "voice entry" is an API implementation detail, not a domain term.
- **"field definition"** and **"field"** were used interchangeably in the conversation. Prefer **Field** in domain discussions; "field definition" is an implementation term describing the schema row, not the concept itself.
