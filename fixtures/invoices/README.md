# Invoice fixtures

Real invoice PDFs live here locally and are git-ignored (they contain PII).
Only two kinds of files are committed, per platform folder (see ADR 0009):

- `*.redacted.txt`: positioned words extracted from a PDF, with names, addresses, phone numbers, and order and
  invoice numbers replaced (`./gradlew :server:parsing:extractFixture`).
- `*.expected.json`: the golden parse result for that fixture.
