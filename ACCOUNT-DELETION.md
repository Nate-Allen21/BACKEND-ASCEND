# ASCEND account deletion and data handling

`DELETE /api/users/me` identifies the account only from the bearer session, requires the current password or a Google ID token issued within the last 10 minutes for the linked Google subject, the explicit phrase `EXCLUIR MINHA CONTA`, and the permanent deletion acknowledgement. The password is checked by the existing PBKDF2 verifier. No password, token, or reason is logged. The operation runs in one database transaction; a failed statement rolls back both the deletes and user removal.

## Data removed

The transaction removes the account, all authentication sessions, profile and profile image, posts, comments authored by the account (including replies to those comments), likes, follows/friend/block relationships, calendar events, quests, mission completion and attributes, focus sessions, virtual currency history, shop and cosmetic purchase history, cosmetic ownership/equipment, and cosmetic audit rows where the account is the actor or a test-balance target. Limited catalog items and founder/cosmetic definitions are not modified. A new account has a new ID and does not regain prior ownership or founder recognition.

## ASCEND CONNECT

Messages authored by the deleting account are deleted because free text can contain identifying information. Messages from other participants remain. The Connect migration makes only the conversation participant references nullable with `ON DELETE SET NULL`; sender references remain required because messages authored by the deleted account are removed in the same transaction. The remaining participant sees `Usuário excluído`, and the API rejects new messages to a conversation whose other participant was removed. This is pseudonymization of the conversation relationship, not a claim that retained message content is anonymous. Participants can still identify themselves or others from their own messages.

## Retention, backups, and legal review

This implementation retains no new per-account deletion record and does not intentionally retain the deleting user's application records. The existing database backup lifecycle may retain pre-deletion copies until each backup expires; the project does not currently define or enforce a deletion-specific backup expiry or restore tombstone. Restores therefore need an operational process to replay account deletion requests before restored data is served. The service's local in-memory failed-password limiter resets on restart and is not shared among multiple instances. These limitations need operational and privacy review. Retention obligations for financial or security records need a verified schema and policy decision; no such separate financial ledger was found in the current ASCEND JPA model.

## Privileged accounts and schema boundary

IDs in `ASCEND_COSMETIC_ADMIN_IDS` are blocked from deletion until an operator transfers that configured privilege. The current application has no persisted admin role, audited transfer flow, or founder account lock, so that environment-based admin transfer remains an operational prerequisite. The code only touches ASCEND tables represented in the current backend model. On 2026-11-26, the production `BACKEND-ASCEND` schema and foreign keys to `users` were inspected; the conversation anonymization migration was applied and its nullable columns and `ON DELETE SET NULL` constraints were verified. Public tables have RLS enabled; the backend connects directly to PostgreSQL rather than using the Supabase client Data API.
