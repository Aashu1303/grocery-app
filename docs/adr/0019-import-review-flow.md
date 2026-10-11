# 0019. Invoice import and review flow on the client

Status: Accepted
Date: 2026-10-11

## Context
Design-plan M2 asks for an import review screen, and section 7.3 asks for loading, empty, error and content
states on every screen. Two things were unsettled: how a PDF gets from the phone's file chooser to the upload, and
whether the review screen holds edits locally until a "save".

## Decision
- **Picking PDFs** is the one new `expect`/`actual` in `feature:invoiceimport` (architecture rule 7):
  `rememberPdfPicker { files -> … }` returns a `PdfPicker`. Android uses `OpenMultipleDocuments`; iOS uses
  `UIDocumentPickerViewController` with copied documents. Both read the bytes off the main thread or into memory
  before calling back. Desktop does nothing, because it only serves screenshots. The view model never sees a
  platform type: it receives `InvoiceFile` values.
- **Limits are checked before uploading** by `ImportInvoicesUseCase`: at most 5 files and 10 MiB in total (the
  server's 15 MiB Base64 body, ADR 0018), no empty files, and no cooks.
- **The review screen saves each change straight away.** The server is authoritative, so there is no local dirty
  state and no "Save changes" button. Editing an item, or accepting the calculated total, is one idempotent `PATCH`.
  A mismatch warning disappears when the lines add up. "Done" just leaves the screen.
- **Warnings are derived on the client** with the same `BillRules.warnings` as the server, so the screen never
  disagrees with it.
- **Two design-system additions:** `NoticeBanner` (info, warning and danger, each with its own icon) for import
  warnings, and `FileRow` for a picked file. `AppKeyboard.DECIMAL` is added for amount fields.
- **Deleting items is not part of M2.** Zero-value promo lines are already dropped by the parser. Removal arrives
  with the full bill editor in M4, where it needs a confirmation.

## Consequences
- A failed save never loses what the user typed: the sheet stays open with their text and a message.
- The Bills tab shows an "Import invoice" action (members and owners only) until the bill list exists in M4.
