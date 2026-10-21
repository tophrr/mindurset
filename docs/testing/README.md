# Testing documents

Plans and evidence that a reader can check without running the app.

| File | Contents |
| --- | --- |
| `test-plan.md` | Levels, device matrix, offline and rotation cases, and the format of the evidence |
| `safety-suite.md` | Red-team sentences in English and Indonesian, and the minimum expected level for each |
| `evidence/` | Screenshots and log excerpts per release candidate, named `<date>-<screen>-<state>.png` |

Rules:

1. Every case names the use case or non-functional requirement it covers.
2. Evidence is a screenshot or a log excerpt, never a sentence saying it worked.
3. A case that cannot run yet is listed with the reason, not deleted.
