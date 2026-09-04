package br.com.bikeshop.model

import br.com.bikeshop.util.Formatador
import java.math.BigDecimal

// Produto da loja. quantidadeEstoque é val: o estoque só muda pelo banco (ProdutoRepository).
class Produto(
    val id: Int?,
    val nome: String,
    val descricao: String?,       // opcional
    val precoCusto: BigDecimal,   // quanto a loja paga ao fornecedor
    val precoVenda: BigDecimal,   // quanto a loja cobra do cliente
    val quantidadeEstoque: Int,
    val ativo: Boolean = true
) : Validavel {

    override fun validar(): List<String> {
        val erros = mutableListOf<String>()
        if (nome.trim().length < 2) erros.add("Nome do produto deve ter pelo menos 2 caracteres.")
        if (precoCusto < BigDecimal.ZERO) erros.add("Preço de custo não pode ser negativo.")
        if (precoVenda <= BigDecimal.ZERO) erros.add("Preço de venda deve ser maior que zero.")
        if (precoVenda < precoCusto) erros.add("Preço de venda menor que o custo (daria prejuízo).")
        if (quantidadeEstoque < 0) erros.add("Estoque não pode ser negativo.")
        return erros
    }

    fun temEstoque(quantidade: Int): Boolean {
        return quantidadeEstoque >= quantidade
    }

    override fun toString(): String {
        return "#$id - $nome - venda ${Formatador.moeda(precoVenda)} - custo ${Formatador.moeda(precoCusto)} - estoque: $quantidadeEstoque"
    }
}
