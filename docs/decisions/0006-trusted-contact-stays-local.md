# 0006 — The trusted contact is stored locally and never exported

- **Status:** Accepted
- **Date:** 13 November 2026
- **Related:** `docs/testing/safety-suite.md` (S-91), ADR 0005, test plan DATA-06

## Context

S-91 asks the crisis card to offer a trusted contact: a person the user can call or message while the
app is halted. The number is not the user's own data — it belongs to someone who never chose to install
this app.

Two decisions follow: where the contact lives, and whether it travels.

## Decision

1. **Storage is the existing `trusted_contact` table**, which the schema already carries, keyed by
   `userId` and read through `TrustedContactRepository`. No migration, no DataStore key. The table is
   wiped by the same "delete all local data" path that wipes check-ins and journal entries.
2. **The contact never leaves the device.** It is excluded from the JSON export (test plan DATA-06):
   exporting would copy a third party's phone number to whatever file the user shares. The exclusion is
   recorded in `TrustedContactRepositoryImpl` so the next person to build the export meets it there.
3. **One contact, not a list.** The sheet shows the first saved contact and offers editing. A list needs
   ordering, deletion and its own screen; none of that is in the proposal.
4. **Nothing on this path touches the network.** The crisis card must work when the connection, the
   Worker, and the model do not.

## Consequences

- DATA-06 becomes "profile, check-ins, journal, actions — and explicitly not the trusted contact". The
  test plan says so where the export is described.
- The dialer and the messaging app are reached through `ACTION_DIAL` and `ACTION_SENDTO`, so the app
  needs neither the `CALL_PHONE` nor the `SEND_SMS` permission, and the number is never dialled without
  the user pressing the dial button.
- A contact with fewer than four digits is refused: a number that cannot be dialled is worse than no
  contact at all on a halt screen.
