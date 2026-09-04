package br.com.bikeshop.model

import br.com.bikeshop.util.Formatador
import java.math.BigDecimal
import java.time.LocalDateTime

// Ordem de serviço (manutenção). dataConclusao e movimentacaoId ficam null até concluir.
class OrdemServico(
    val id: Int?,
    val clienteId: Int,
    val clienteNome: String?,
    val mecanicoId: Int,
    val mecanicoNome: String?,
    val descricao: String,
    val valor: BigDecimal,
    val status: StatusOrdemServico,
    val dataAbertura: LocalDateTime?,
    val dataConclusao: LocalDateTime?,
    val movimentacaoId: Int?
) : Validavel {

    override fun validar(): List<String> {
        val erros = mutableListOf<String>()
        if (descricao.trim().length < 5) erros.add("Descreva o serviço com pelo menos 5 caracteres.")
        if (valor <= BigDecimal.ZERO) erros.add("Valor do serviço deve ser maior que zero.")
        return erros
    }

    override fun toString(): String {
        return "OS #$id [$status] ${Formatador.moeda(valor)} - cliente: ${clienteNome ?: clienteId}" +
            " - mecânico: ${mecanicoNome ?: mecanicoId} - aberta em ${Formatador.dataHora(dataAbertura)}" +
            " - concluída em ${Formatador.dataHora(dataConclusao)} - $descricao"
    }
}
