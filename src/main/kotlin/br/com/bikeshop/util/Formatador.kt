package br.com.bikeshop.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Formata dinheiro e data no padrão brasileiro
object Formatador {

    private val formatoMoeda: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    private val formatoDataHora: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    // 1234.5 -> "R$ 1.234,50"
    fun moeda(valor: BigDecimal): String = formatoMoeda.format(valor)

    // "04/09/2026 14:05"; se for null mostra "-"
    fun dataHora(valor: LocalDateTime?): String = valor?.format(formatoDataHora) ?: "-"
}
