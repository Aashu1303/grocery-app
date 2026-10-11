# 0020. Pantry from bill lines, categories and expiry estimates

Status: Accepted
Date: 2026-10-11

## Context
M3 turns imported bills into a live pantry (design-plan 6.4 items 6 and 7). The plan fixes the idea (HSN chapter plus
keywords give a category and "is food"; the category gives a default place and shelf life) but not when pantry items
appear, how a person fixes a wrong guess, or how editing works.

## Decision
- **Pantry items are created when a bill is imported**, not when it is approved. The groceries are in the flat
  whether or not the split is agreed yet, and approval arrives in M4. Items from the same bill line are never created
  twice (a unique source line).
- **Categories** are `PRODUCE, DAIRY, EGGS_MEAT, BAKERY, STAPLES, SNACKS, BEVERAGES, CONDIMENTS, FROZEN` (food) and
  `HOUSEHOLD, PERSONAL_CARE, OTHER` (not food). `Categorizer` tries keyword rules on the item name first (processed
  foods before the raw ingredient they contain, so "potato chips" is a snack), then the HSN chapter, then a platform
  hint (Handpickd sells only produce). Whatever is left is `OTHER`, not food, until the user says so.
- **The user can override "food or not"** per bill line in the review sheet. Marking a line as food adds its pantry
  item; marking it as not food removes the item if nobody has used or discarded it yet. Editing a line's name, amount
  or count never touches its pantry item, and removing a bill line leaves the item in the kitchen.
- **Quantity** of an item is the line's pack size or weight times its count, or just the count when the invoice has no
  measure. People type quantities in pieces, g, kg, ml or L (`InputUnit`); the app converts them exactly to the base
  unit.
- **Expiry** is an estimate from `ExpiryRules`: a shelf life per category with a few name-specific overrides (milk 2
  days, leafy greens 3, eggs 21, potatoes and onions 21). A manual item with no date gets the same estimate. The date
  can be moved a day at a time in the item sheet; a date picker is not needed for that. Storage places follow the same
  rules (dairy and most produce to a fridge, frozen food to a freezer, the rest on the shelf).
- **API.** `GET /households/{id}/pantry` lists items still in stock (`?status=` takes `FINISHED`, `DISCARDED` or
  `ALL`), `POST` adds one, and `PATCH /households/{id}/pantry/{itemId}` changes it. The plan listed PATCH on the
  collection, which cannot name an item. Cooks may edit the pantry (they run the kitchen); outsiders and items of other
  households get 404.

## Consequences
- A wrong guess costs one tap in the review sheet instead of a stuck item in the pantry.
- Category rules are plain keyword lists, easy to extend and test; they will miss unfamiliar items, which then stay out
  of the pantry until marked as food.
