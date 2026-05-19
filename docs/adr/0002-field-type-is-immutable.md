# Field Type is immutable after creation

A Field's type cannot be changed once the Field exists. If a user wants a different type, they must delete the Field (orphaning existing values per ADR-0001) and add a new one.

Allowing type changes would require coercing or validating all existing Field Values in every Entry against the new type — TEXT values like "Great place!" would silently become invalid NUMBER values. The complexity and data-consistency risk outweigh the convenience. Immutability keeps the contract simple: the type declared at creation is permanent.
