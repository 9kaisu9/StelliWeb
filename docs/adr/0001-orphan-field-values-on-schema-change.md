# Orphan field values when a field is removed from a schema

When a Field is removed from a List's Schema, existing Entries are not modified. The Field Values for that Field remain in the Entry's JSON blob, keyed by the deleted Field's ID. They are silently unreachable — never displayed, never validated — but not deleted.

The alternative was a bulk update across all Entries in the List to scrub the orphaned keys. We rejected this because it adds a costly write operation for zero user-visible benefit, and contradicts the simplicity that motivated storing Field Values as a JSON blob in the first place. A future developer seeing stale keys in an Entry's blob should not treat them as a bug.
