---
name: add-invoice-parser
description: Add or fix a platform invoice parser in server/parsing (Zepto, Blinkit, Instamart, Handpickd or a new platform), including redacted fixtures and golden tests. Use when an invoice format is new or a parser fails.
---

# Add an invoice parser

Pipeline (design-plan 6.5): extract text with positions (PDFBox), detect the platform, parse with a template into
`ParsedInvoice`, fall back to the LLM, normalize, reconcile, return a draft bill.

## 1. Create a redacted text fixture (never commit the PDF)

1. Put the real PDF under `fixtures/invoices/<platform>/` locally. It is git-ignored (ADR 0009).
2. Extract its positioned text with the project's extractor (or `pdftotext -layout` for a first look).
3. Replace every customer name, address, phone number, email, GSTIN of the buyer, order number and invoice number
   with obvious placeholders (`CUSTOMER NAME`, `ORDER-0001`, `INV-0001`). Keep the seller name, line items, HSN codes,
   quantities and amounts exactly.
4. Save as `fixtures/invoices/<platform>/<case>.redacted.txt`. Only `*.redacted.txt` passes `.gitignore`.
5. Run `./scripts/check-no-real-invoices.sh`.

## 2. Write the golden test first

- Add `<case>.expected.json` next to the fixture with the expected `ParsedInvoice`: header (platform, invoice no,
  date), lines (raw name, quantity, unit, amount paid in paise, HSN, UPC), charges (kind, signed paise), total.
- Add the case to the parser golden test in `server/parsing/src/test`. It must fail before you write the parser.
- Assert reconciliation: lines + charges − discounts + round-off == total.

## 3. Implement `InvoiceParser`

```kotlin
class <Platform>InvoiceParser : InvoiceParser {
    override val platform = Platform.<PLATFORM>
    override fun matches(document: ExtractedDocument): Boolean // seller/footer signature, never the file name
    override fun parse(document: ExtractedDocument): ParseResult // AppResult-style, typed failures
}
```

Rules from the samples:
- Rebuild rows from text positions; descriptions wrap across lines.
- Handle multi-page tables with repeated headers.
- Drop zero-value promo lines; keep non-food lines (they are splittable but never enter the pantry).
- The split basis is the amount actually paid per line (tax-inclusive total, or amount minus line promo).
- Bill-level fees and discounts become charge lines.
- Parse money with `Money.parse`, never floating point.

Register the parser in the detector's list and add a detection test.

## 4. Finish

`./gradlew :server:parsing:test`, then `./gradlew check` and the `pre-commit-review` skill. Note the platform's
quirks in the plan's section 2 table if they are new.
