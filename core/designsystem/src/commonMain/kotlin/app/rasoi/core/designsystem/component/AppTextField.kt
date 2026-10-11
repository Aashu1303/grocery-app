package app.rasoi.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.rasoi.core.designsystem.theme.AppTheme

/** Labelled single-line input. Errors show inline and the typed value is kept (design-plan 7.3, rule 9). */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    keyboard: AppKeyboard = AppKeyboard.TEXT,
    enabled: Boolean = true,
    onDone: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        label = { Text(label, style = AppTheme.type.label) },
        supportingText = (error ?: supportingText)?.let { message -> { Text(message, style = AppTheme.type.caption) } },
        isError = error != null,
        singleLine = true,
        textStyle = if (keyboard == AppKeyboard.TEXT) AppTheme.type.body else AppTheme.type.numeric,
        shape = AppTheme.shapes.small,
        keyboardOptions = KeyboardOptions(
            keyboardType = when (keyboard) {
                AppKeyboard.TEXT -> KeyboardType.Text
                AppKeyboard.PHONE -> KeyboardType.Phone
                AppKeyboard.NUMBER, AppKeyboard.CODE -> KeyboardType.Number
            },
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline,
            errorBorderColor = colors.danger,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.onSurfaceVariant,
            errorLabelColor = colors.danger,
            errorSupportingTextColor = colors.danger,
            unfocusedSupportingTextColor = colors.onSurfaceVariant,
            focusedSupportingTextColor = colors.onSurfaceVariant,
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            cursorColor = colors.primary,
        ),
    )
}
