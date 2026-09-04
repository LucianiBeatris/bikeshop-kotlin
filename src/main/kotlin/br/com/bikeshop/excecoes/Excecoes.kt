package br.com.bikeshop.excecoes

import java.math.BigDecimal

// Exceções próprias do sistema

// Erro de regra de negócio (ex.: cliente não encontrado, venda sem itens).
open class NegocioException(mensagem: String) : Exception(mensagem)

// Saída que deixaria o caixa negativo.
class SaldoInsuficienteException(valorPedido: BigDecimal, saldoAtual: BigDecimal) :
    NegocioException("Saldo insuficiente no caixa: tentou pagar R$ $valorPedido mas o saldo é R$ $saldoAtual")

// Venda de mais unidades do que tem no estoque.
class EstoqueInsuficienteException(produto: String, pedido: Int, disponivel: Int) :
    NegocioException("Estoque insuficiente para '$produto': pedido $pedido, disponível $disponivel")

// Usuário digitou "cancelar" no meio de uma operação.
class OperacaoCanceladaException : Exception("Operação cancelada pelo usuário")
