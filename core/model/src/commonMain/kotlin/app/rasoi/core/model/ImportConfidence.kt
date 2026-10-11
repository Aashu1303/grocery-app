package app.rasoi.core.model

/** How far the review screen can trust an imported bill: LOW means the LLM fallback read it (ADR 0017). */
enum class ImportConfidence { HIGH, MEDIUM, LOW }
