---
name: add-invoice-parser
description: Add or fix a platform invoice parser in server/parsing (Zepto, Blinkit, Instamart, Handpickd or a new platform), including redacted fixtures and golden tests. Use when an invoice format is new or a parser fails.
---

# Add an invoice parser

Pipeline (design-plan 6.5, ADR 0017): `PdfTextExtractor` (positioned words) → `InvoicePipeline` picks the first
`InvoiceParser` whose `matches` is true → template `parse` → `Normalizer` → `Reconciler` → `ParseResult` with
warnings and confidence. `InvoiceFallback` handles documents no template reads.

## 1. Make a redacted fixture (never commit the PDF)

1. Put the real PDF under `fixtures/invoices/<platform>/` locally. Everything there except `*.redacted.txt`,
   `*.expected.json` and the README is git-ignored (ADR 0009).
2. Extract and redact in one step. Pass every customer name, address word, phone number, email, order number and
   invoice number, plus the seller's street address:
   ```sh
   ./gradlew :server:parsing:extractFixture --args="/abs/path/in.pdf \
     /abs/path/fixtures/invoices/<platform>/<case>.redacted.txt 'Real Name=>CUSTOMER' 'OD123=>OD-DEMO0001'"
   ```
   The tool refuses to write a fixture that still contains a replaced value. Never write the replacement pairs to a
   file or a commit message: they are the PII.
3. Read the whole fixture yourself (`awk '{$1=$2=$3=$4=""; print}'`) and confirm that no name, address, phone,
   email, PIN code or real order or invoice number remains. Keep the seller name, line items, HSN codes, quantities
   and amounts exactly.
4. Run `./scripts/check-no-real-invoices.sh` and `git status --ignored fixtures/` (the PDF must show as ignored).

## 2. Write the golden snapshot first

- Add `<case>.expected.json` beside the fixture in the shape that `GoldenInvoiceTest.snapshot` writes: platform,
  invoice number, order id, date, lines (rawName, units, amount, measure, mrp, hsn, upc), charges (kind, label,
  signed amount), total, and warnings, which must be empty.
- Work out the amounts by hand from the PDF. `./gradlew :server:parsing:test` must fail before the parser exists.

## 3. Implement `InvoiceParser`

```kotlin
class <Platform>InvoiceParser : InvoiceParser {
    override val platform = Platform.<PLATFORM>
    override fun matches(document: ExtractedDocument): Boolean = // seller or structural signature, never a file name
    override fun parse(document: ExtractedDocument): AppResult<ParsedInvoice> = parsing {
        val layout = Layout(document)
        val invoiceNumber = layout.valueAfter("Invoice No:").orFail("invoice_number")
        // ...
    }
}
```

- `parsing {}` with `orFail("field")` turns anything missing into `unreadable_<field>`, never an exception or a
  guessed value. An invoice with no lines is unreadable.
- Use `Layout` (lines from positions), `DescriptionAssigner` for wrapped descriptions (`CENTERED` or `TOP`),
  `parseAmount`, `parseDate`, `parseMeasure` and `ChargeClassifier`. Money is always `Money`, never floating point.
- Handle multi-page tables with repeated headers; columns may shift per page.
- `ParsedLine.amount` is the split basis: what was paid for the line (see the table in ADR 0017). Bill-level fees,
  discounts and round-off become `ParsedCharge`s. Rows that only restate line totals are not charges.
- Register the parser in `InvoicePipeline.default()` and add `Platform.<PLATFORM>` if it is new.

## 4. Finish

- `TemplateDetectionTest` must show that every parser matches only its own fixtures and that a truncated document
  is unreadable.
- Run `./gradlew :server:parsing:test`, then `./gradlew check` and the `pre-commit-review` skill.
- New platform quirks go in the plan's section 2 table; a new split basis goes in ADR 0017 or a new ADR.
