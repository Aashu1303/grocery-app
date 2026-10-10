package app.rasoi.buildlogic

/** Features use design-system tokens, components and string resources only (design-plan section 7). */
object SourceRules {
    private val rules = listOf(
        Regex("""\bColor\(""") to "raw colour; use AppTheme.colors",
        Regex("""\b\d+(\.\d+)?\.(dp|sp)\b""") to "raw size; use AppTheme.spacing or tokens",
        Regex("""^import androidx\.compose\.material3\.""", RegexOption.MULTILINE) to
            "Material component; use a design-system component",
        Regex("""\bText\(\s*"""") to "hard-coded string; use a string resource",
    )

    fun violations(path: String, source: String): List<String> = source.lineSequence().flatMapIndexed { index, line ->
        rules.filter { (regex, _) -> regex.containsMatchIn(line) }.map { (_, why) -> "$path:${index + 1}: $why" }
    }.toList()
}
