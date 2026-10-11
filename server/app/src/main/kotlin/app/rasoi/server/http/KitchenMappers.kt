package app.rasoi.server.http

import app.rasoi.core.contract.kitchen.DishDto
import app.rasoi.core.contract.kitchen.FreshnessDto
import app.rasoi.core.contract.kitchen.UseFirstEntryDto
import app.rasoi.core.domain.kitchen.DishSuggestion
import app.rasoi.core.domain.kitchen.UseFirstEntry

internal fun UseFirstEntry.toDto() = UseFirstEntryDto(item.toDto(), daysLeft, FreshnessDto.valueOf(freshness.name))

internal fun DishSuggestion.toDto() = DishDto(name, uses, summary)
