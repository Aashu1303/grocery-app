# 0001. Parse invoice PDFs on the server

Status: Accepted
Date: 2026-10-10

## Context
Four platforms use four layouts, and wrapped rows need text positions. Parsers change when platforms change their invoices; shipping that logic in the app would need an app release each time.

## Decision
Apache PDFBox runs on the server and extracts text with positions. Template parsers and the LLM fallback live in `server/parsing`. The app uploads PDFs and receives a draft bill.

## Consequences
Parser fixes ship without an app release, and API keys for the LLM stay off devices. Import needs a network connection. Only line-item text is ever sent to the LLM.
