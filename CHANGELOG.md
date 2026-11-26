# Changelog

## 1.2 (release candidate)

- Improve live chart interpolation, animation and per-frame path reuse.
- Resolve visible installed application identities and icons, cache usage results, and add ranked app details.
- Redesign analytics, calendar selection, tools and settings navigation.
- Add app loading placeholders, retry actions, radio signal indicators and connection event styling.
- Add chart gap regression tests. Physical device performance and accessibility validation remain pending.


## 1.1 (release candidate)

- Add dashboard live graphs, usage charts, per-app statistics and daily mobile limits.
- Add optional network tests, connection history, Wi-Fi and cellular analysis, and local DNS-SD discovery.
- Add floating speed overlay, Quick Settings controls, configurable notification styles and dashboard personalization.
- Add usage forecasts, screen-state estimates, CSV/JSON exports and daily usage backup/restore.
- Serialize usage queries, preserve saved daily counters, reduce unnecessary preference writes and restrict restart receivers.
- Expand unit coverage and document privacy, permission and release limitations.

Build numbers remain monotonic within the existing GitHub workflow. Existing APKs use the shared test signing key; this is not a Play Store release.
