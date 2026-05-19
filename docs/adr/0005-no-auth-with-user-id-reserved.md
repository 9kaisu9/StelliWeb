# No authentication in v1, with user_id reserved in the schema

v1 has no authentication layer. Spring Security has been removed. The app is single-user by design — it runs locally on the owner's machine, not exposed to the public internet.

Despite this, a `user_id` foreign key is present on both `lists` and `entries` from day one, and a single default user (id=1) is seeded on startup. This is deliberate future-proofing: adding multi-user support later without `user_id` would require a painful schema migration across every data table. With `user_id` already in place, adding auth and multi-tenancy is a matter of wiring up login and filtering queries — the data model doesn't change.

A future developer seeing `user_id` and no auth should not remove it. The column is load-bearing for future work, not vestigial.
