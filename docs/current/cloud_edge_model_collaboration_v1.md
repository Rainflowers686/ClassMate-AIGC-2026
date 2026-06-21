# BlueLM Cloud + Edge Model Collaboration

Date: 2026-06-21

No credential, endpoint, request body, or `config.local.json` content is recorded.

## Routing Contract

ClassMate uses this route for learning assets:

```text
BLUE_LM_CLOUD -> EDGE_3B -> LOCAL_RULE
```

| Route | Product role | Fallback rule |
| --- | --- | --- |
| BLUE_LM_CLOUD | Organize classroom material into summary, knowledge points, quiz, review plan, and grounded Ask results. | If unavailable, try edge 3B for basic explanation and review guidance. |
| EDGE_3B | Weak-network, offline, and privacy-sensitive fallback for summary, quiz draft, review advice, and export draft. | If device resources are unavailable, local rules still publish a minimum study loop. |
| LOCAL_RULE | Guaranteed minimum path for evidence, quiz templates, review queue, diagnosis, and study pack export. | Never blocks the user from continuing. |

## CapabilityPlan Fields

Each plan step should explain:

- `primaryModelRoute`
- `fallbackModelRoute`
- reason
- output surface
- user-visible benefit
- risk level
- whether confirmation is required

User-facing copy should describe learning outcomes, not internal technical plumbing.

## Current User-Visible Benefits

- Text/material input: cloud model organizes class highlights; edge/local fallback keeps summary, quiz, and review usable.
- OCR/image input: image text becomes evidence; manual confirmation remains available.
- Document input: document snippets and page hints become traceable evidence.
- Audio input: transcript becomes timeline evidence; dialect mode marks uncertain segments.
- WrongBook/Review: similarity and mastery signals adjust review priority.
- Export: study pack includes cloud/edge/local route note without internal provider details.
