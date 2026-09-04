package br.com.bikeshop.model

import br.com.bikeshop.util.Formatador
import java.math.BigDecimal
import java.time.LocalDateTime

// Uma linha do extrato: valor, quem pagou, quem recebeu, data/hora, descrição e responsável.
// Os campos *Nome são só para mostrar (vêm do JOIN com pessoa), por isso podem ser null.
data class MovimentacaoFinanceira(
    val id: Int?,
    val tipo: TipoMovimentacao,
    val origem: OrigemMovimentacao,
    val valor: BigDecimal,
    val pagadorId: Int,
    val recebedorId: Int,
    val responsavelId: Int,
    val dataHora: LocalDateTime,
    val descricao: String,
    val pagadorNome: String? = null,
    val recebedorNome: String? = null,
    val responsavelNome: String? = null
) {
    override fun toString(): String {
        val sinal = if (tipo == TipoMovimentacao.ENTRADA) "+" else "-"
        return "#$id ${Formatador.dataHora(dataHora)} $sinal${Formatador.moeda(valor)} [$origem] $descricao" +
            " | pagou: ${pagadorNome ?: pagadorId} | recebeu: ${recebedorNome ?: recebedorId}" +
            " | responsável: ${responsavelNome ?: responsavelId}"
    }
}
