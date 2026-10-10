# 0002. Money as Long paise, quantities as Long base units

Status: Accepted
Date: 2026-10-10

## Context
Floating point cannot represent rupee amounts exactly, and splits must sum to the paise.

## Decision
`Money` is a value class over `Long` paise. Quantities are `Long` base units (mg, ml, or count x 1000) with a `Unit` enum. Every split uses largest-remainder rounding.

## Consequences
No `Float` or `Double` for money or quantity anywhere. Display formatting uses Indian digit grouping and lives in one place.
