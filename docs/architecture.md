# Coord architecture (v1)

## Revision model

Features and tasks keep a pointer to the latest accepted revision. Every publish creates an immutable `Revision` row with:

- full JSON snapshot of the work item
- actor name + source (`web` | `api` | `agent`)
- change message
- parent revision id
- monotonic version number

Comments are separate and never mutate the current revision.

## Identity (temporary)

No JWT. Clients send:

- `X-Actor-Name` (default `anonymous`)
- `X-Actor-Source` (default `api`)
