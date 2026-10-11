package app.rasoi.core.designsystem.component

import androidx.compose.runtime.Immutable

@Immutable
data class ChoiceOption<T>(val value: T, val label: String)
