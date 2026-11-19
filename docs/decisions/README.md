# Decisions

One file per decision, numbered, never edited after it is accepted. If a decision changes, write a
new file that supersedes the old one and add `Supersedes: NNNN` to its header. The point is that a
reader six weeks from now can see what we knew and when, not what we wish we had decided.

## Format

```
# NNNN: <decision in one line>

Date:
Status: proposed | accepted | superseded by NNNN
Deciders:

## Context
## Decision
## Consequences
## Alternatives considered
```

## Index

| # | Decision | Status |
| --- | --- | --- |
| 0001 | Repository conventions, access model and quality gates | accepted |
| 0002 | Review gate rollout, staged | accepted |
| 0003 | Keyword retrieval before a vector index | superseded by 0008 |
| 0005 | Fail closed where a classifier runs | accepted |
| 0006 | The trusted contact stays local | accepted |
| 0007 | The ASQ step asks the published items and keeps no answers | accepted |
| 0008 | A vector index for retrieval, and when the build catches up | accepted |
