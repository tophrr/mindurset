## What this changes

<!-- One paragraph. What behaviour changes, for whom, and why now. -->

## Traceability

Closes: `F-`, `UC-`

<!-- A pull request that closes no traceability row is sent back. See docs/decisions and the matrix. -->

## How it was checked

- [ ] `flock /tmp/mindurset-gradle.lock ./gradlew spotlessCheck :app:lintDebug :app:testDebugUnitTest :app:assembleDebug`
- [ ] A new or updated test covers this change (or the reason it cannot be tested is written below)
- [ ] Screenshot attached for any UI change
- [ ] No new warning in the build log
- [ ] No secret, keystore or `local.properties` in the diff

## Notes for the reviewer

<!-- Risks, trade-offs, the part you are least sure about. -->

## AI assistance

<!-- Assisted-by: <tool name>. Delete this section if no agent was involved. -->
