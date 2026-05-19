# Stelli

A locally-run, self-hosted personal list manager. Users define custom list types with typed fields, add entries manually or by voice, and keep all data on their own machine.

## Language

### Core model

**List**:
A user-defined collection with a named schema of typed fields. The fundamental organisational unit.
_Avoid_: Category, list type, database, table

**Schema**:
The complete ordered set of Fields belonging to a List.
_Avoid_: Structure, template (when referring to an existing list's shape)

**Field**:
A named, typed slot in a List's Schema. Defined once per List; shared across all Entries in that List.
_Avoid_: Column, attribute, property, field definition

**Entry**:
A single record in a List, holding one Field Value per Field.
_Avoid_: Item, record, row, submission

**Field Value**:
The actual data stored for one Field within one Entry.
_Avoid_: Value, cell, data

**Template**:
A List seeded by the system on first boot. Once created it is indistinguishable from a user-created List.
_Avoid_: Preset, example, default

### Field types

**Field Type**:
The kind of data a Field holds. One of: TEXT, NUMBER, RATING, DATE, BOOLEAN, IMAGE, VIDEO, LOCATION, OPTION, MULTI_OPTION.
_Avoid_: Data type, column type

**Location Value**:
The stored form of a LOCATION Field Value: `{lat, lng, label}` — decimal coordinates plus a human-readable display string (place name or address) populated by the frontend from whichever Maps API the client uses. Coordinates are used to render a map pin or construct a Maps URL; the label is used for display in list views without requiring an API call.
_Avoid_: Address, place, coordinates (alone)

**Rating**:
A numeric score on a fixed 1–5 scale, used as a Field Type.
_Avoid_: Score, stars

**Option**:
A single string value chosen from a Field's predefined Choices, used as a Field Type.
_Avoid_: Dropdown, single select

**Multi-Option**:
One or more string values chosen from a Field's predefined Choices, used as a Field Type.
_Avoid_: Multiselect, tags, checkboxes

**Choice**:
One allowed string value in the predefined set for an Option or Multi-Option Field.
_Avoid_: Option value, pick, selection

### Voice entry

**Voice Memo**:
An audio recording submitted by the User to create an Entry.
_Avoid_: Audio clip, recording, voice note

**Transcript**:
The text produced by Whisper from a Voice Memo.
_Avoid_: Transcription, text output

**Field Extraction**:
The AI process of mapping Transcript content to Field Values for a specific List's Schema.
_Avoid_: Parsing, inference, NLP processing

**Partial Fill**:
The pre-populated set of Field Values returned after Field Extraction, containing only confidently identified values. Fields not mentioned in the Transcript are absent — never guessed. IMAGE, VIDEO, and LOCATION Fields are always absent from a Partial Fill; they cannot be extracted from speech.
_Avoid_: Pre-fill, suggestion, draft

### People

**User**:
The person who owns and manages Lists and Entries on a local Stelli instance. Single-user in v1.
_Avoid_: Person, owner, account, customer

## Relationships

- A **List** has exactly one **Schema** (its ordered set of **Fields**)
- A **Field** belongs to exactly one **List** and has exactly one **Field Type**
- An **Option** or **Multi-Option** **Field** has one or more **Choices**
- An **Entry** belongs to exactly one **List** and holds one **Field Value** per **Field** — plus potentially orphaned Field Values for Fields that have since been removed from the Schema
- A **Voice Memo** is processed into a **Transcript**, which is subjected to **Field Extraction** to produce a **Partial Fill**
- A **Template** is a **List** seeded by the system; once created it behaves identically to any user-created **List**

**Orphaned Field Values**: When a Field is removed from a Schema, its stored values in existing Entries are retained in the JSON blob but never surfaced. They are not deleted, and not an error.

**Required Fields**: A Field marked required must have a value present in any Entry creation or update request. The API returns 400 if a required Field Value is missing.

**Choice Validation**: On Entry creation and update, OPTION and MULTI_OPTION Field Values must match one of the Field's defined Choices. The API returns 400 if an unrecognised value is submitted. Existing Entry values are not retroactively invalidated when Choices change — orphaned choice values follow the same rule as orphaned Field Values (see ADR-0001).

## Example dialogue

> **Dev:** "When a User submits a Voice Memo for the Restaurants List, does the system immediately create an Entry?"

> **Domain expert:** "No — the backend returns a Partial Fill: Field Values the AI confidently extracted. The User reviews it and decides to save. Only then is the Entry created."

> **Dev:** "What if the Transcript didn't mention the Cuisine Field at all?"

> **Domain expert:** "That Field Value is simply absent from the Partial Fill. The User fills it in manually. We never guess."

> **Dev:** "If someone creates a new List — say, Public Toilets — with a Location Field and a Cleanliness Rating Field, that's just a normal List, not a Template?"

> **Domain expert:** "Exactly. A Template is only a List seeded by the system on first boot. Once it exists, it behaves identically to any other List the User created from scratch."

## Flagged ambiguities

- **"field definition"** and **"field"** were used interchangeably. Prefer **Field** in domain discussions; "field definition" is an implementation term for the schema row.
- **"voice entry"** appears in the API path (`/voice-entry`) but is not a domain term. The User submits a **Voice Memo** and receives a **Partial Fill**.
- **"template"** is ambiguous when used loosely to mean "a starting-point schema." Canonical meaning: a **List** seeded by the system. A user copying or modifying a List is not working with a Template.
