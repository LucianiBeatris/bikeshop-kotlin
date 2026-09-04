package br.com.bikeshop.model

import java.math.BigDecimal
import java.time.LocalDateTime

// Item de venda ou de compra (ligação N para N com produto)
data class ItemOperacao(
    val produtoId: Int,
    val produtoNome: String,
    val quantidade: Int,
    val precoUnitario: BigDecimal
) {
    val subtotal: BigDecimal
        get() = precoUnitario.multiply(BigDecimal(quantidade))
}

// Venda para cliente
class Venda(
    val id: Int?,
    val clienteId: Int,
    val clienteNome: String?,
    val vendedorId: Int,
    val vendedorNome: String?,
    val movimentacaoId: Int?,       // preenchido depois que o Caixa registra a entrada
    val dataHora: LocalDateTime?,   // gerado pelo banco
    val total: BigDecimal,
    val itens: List<ItemOperacao> = emptyList()
)

// Compra de fornecedor
class Compra(
    val id: Int?,
    val fornecedorId: Int,
    val fornecedorNome: String?,
    val compradorId: Int,
    val compradorNome: String?,
    val movimentacaoId: Int?,
    val dataHora: LocalDateTime?,
    val total: BigDecimal,
    val itens: List<ItemOperacao> = emptyList()
)
