package app.rasoi.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceRulesTest {
    @Test
    fun cleanFeatureCodePasses() {
        val source = """
            import app.rasoi.core.designsystem.component.AppButton
            AppButton(text = stringResource(Res.string.approve), onClick = onApprove)
            Spacer(Modifier.height(AppTheme.spacing.md))
        """.trimIndent()
        assertEquals(emptyList(), SourceRules.violations("F.kt", source))
    }

    @Test
    fun rawValuesAreReportedWithLineNumbers() {
        val source = """
            import androidx.compose.material3.Button
            val c = Color(0xFF00FF00)
            Spacer(Modifier.height(16.dp))
            Text("Approve")
            val s = 14.sp
        """.trimIndent()
        assertEquals(
            listOf(
                "F.kt:1: Material component; use a design-system component",
                "F.kt:2: raw colour; use AppTheme.colors",
                "F.kt:3: raw size; use AppTheme.spacing or tokens",
                "F.kt:4: hard-coded string; use a string resource",
                "F.kt:5: raw size; use AppTheme.spacing or tokens",
            ),
            SourceRules.violations("F.kt", source),
        )
    }
}
