# 0017. Invoice parsing: positioned-text fixtures, structural detection, per-platform split basis

Status: Accepted
Date: 2026-10-11

## Context
Design-plan sections 2 and 6.5 describe the parsing pipeline. Building it against the four sample invoices showed
four things the plan did not settle:

1. Golden tests need the PDF's text *with positions* (rows are rebuilt from coordinates), but real PDFs never enter
   git (ADR 0009). Plain `pdftotext` output loses the coordinates the parsers depend on.
2. The Handpickd bill never prints "handpickd": the brand is only in the logo image.
3. "Amount actually paid per line" is printed differently by each platform.
4. The plan's `fun parse(document): ParseResult` mixes two results: a template parse that can fail, and the
   pipeline's normalized, reconciled output with warnings.

## Decision
- **Fixture format.** Each fixture is an `ExtractedDocument` in a line-based text form ("rasoi extracted document
  v1": a `page <n> <width> <height>` line, then `<x> <y> <width> <height> <text>` per word). It is produced from a
  local PDF by `./gradlew :server:parsing:extractFixture`. The tool replaces PII words and refuses to write a fixture
  that still contains any replaced value. Seller street addresses are redacted too, because a dark-store address
  narrows down where the customer lives. Each fixture has a committed `<case>.expected.json` golden snapshot, so ADR
  0009's allow-list now has two patterns: `*.redacted.txt` and `*.expected.json`. The test task declares only these
  files as inputs, so the ignored PDFs never affect caching.
- **Detection** stays content-based, never by file name. The signatures are: Instamart, "Swinsta" or "Instamart";
  Zepto, "ZEPTO MARKETPLACE" or "Geddit Convenience"; Blinkit, "Blink Commerce"; Handpickd, its own bill
  structure: "Bill Total", an `Order: OD-` number, and per-kilogram rates.
- **Split basis per platform (`ParsedLine.amount`).**

  | Platform | Basis |
  | --- | --- |
  | Instamart | The Total column |
  | Zepto | Taxable value plus CGST, SGST and cess |
  | Blinkit | The Total column |
  | Handpickd | Line amount plus its promo discount |

  Summary rows that only restate the lines (items total, total promo) are not charges.
- **Typed template results.** `InvoiceParser.parse` returns `AppResult<ParsedInvoice>`. A missing required field
  (table, total, invoice number, date, any item row) becomes `Validation(document: unreadable_<field>)`; there is
  no guessed total. `InvoicePipeline` turns a template result into a `ParseResult` (invoice, warnings,
  confidence). Confidence levels:
  - HIGH: a template parse that reconciles.
  - MEDIUM: a template parse with a total mismatch.
  - LOW: any result from the fallback.

## Consequences
- Fixtures are diffable text, and parsers are tested without PDFBox; `PdfTextExtractor` has its own tests on
  synthetic PDFs.
- A new platform needs a fixture made with `extractFixture`, a snapshot and a parser (`add-invoice-parser` skill).
- Handpickd detection could match another seller with the same structure; the detection matrix test guards the four
  known formats, and an unmatched document goes to the fallback.
