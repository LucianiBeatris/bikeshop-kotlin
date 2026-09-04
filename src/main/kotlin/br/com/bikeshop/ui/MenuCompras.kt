package br.com.bikeshop.ui

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.ItemOperacao
import br.com.bikeshop.model.TipoPessoa
import br.com.bikeshop.repository.PessoaRepository
import br.com.bikeshop.repository.ProdutoRepository
import br.com.bikeshop.service.CompraService
import br.com.bikeshop.util.Console
import br.com.bikeshop.util.Formatador
import java.math.BigDecimal

// Compras de fornecedor
class MenuCompras {

    private val compraService = CompraService()
    private val pessoaRepo = PessoaRepository()
    private val produtoRepo = ProdutoRepository()

    fun executar() {
        while (true) {
            println("\n---------- COMPRAS DE FORNECEDOR ----------")
            println("1 - Registrar compra")
            println("2 - Listar compras")
            println("0 - Voltar")
            val opcao = Console.lerInteiro("Opção", 0, 2)
            if (opcao == 0) return

            executarComSeguranca {
                when (opcao) {
                    1 -> registrar()
                    2 -> listar()
                }
            }
        }
    }

    private fun registrar() {
        println("\n-- Nova compra (digite 'cancelar' para desistir) --")

        val fornecedores = Database.conectar().use { conn -> pessoaRepo.listarPorTipo(conn, TipoPessoa.FORNECEDOR) }
        if (fornecedores.isEmpty()) throw NegocioException("Nenhum fornecedor cadastrado.")
        println("Fornecedores:")
        fornecedores.filter { it.ativo }.forEach { println("  $it") }
        val fornecedorId = Console.lerInteiro("Id do fornecedor", 1)

        val produtos = Database.conectar().use { conn -> produtoRepo.listar(conn) }
        println("Produtos:")
        produtos.forEach { println("  $it") }

        val itens = mutableListOf<ItemOperacao>()
        while (true) {
            val produtoId = Console.lerInteiro("Id do produto (0 para fechar)", 0)
            if (produtoId == 0) break
            val produto = produtos.find { it.id == produtoId }
            if (produto == null) {
                println("  ! Produto $produtoId não existe.")
                continue
            }
            if (itens.any { it.produtoId == produtoId }) {
                println("  ! Produto já está na lista.")
                continue
            }
            val quantidade = Console.lerInteiro("Quantidade", 1)
            val precoUnitario = Console.lerDecimal("Preço unitário pago (sugerido: ${produto.precoCusto})", BigDecimal("0.01"))
            itens.add(ItemOperacao(produtoId, produto.nome, quantidade, precoUnitario))
            println("  + ${produto.nome} x $quantidade = ${Formatador.moeda(precoUnitario.multiply(quantidade.toBigDecimal()))}")
        }
        if (itens.isEmpty()) throw NegocioException("Nenhum item, compra não registrada.")

        val total = itens.sumOf { it.subtotal }
        println("Total da compra: ${Formatador.moeda(total)}")
        if (!Console.confirmar("Confirmar a compra?")) throw NegocioException("Compra não confirmada.")

        val compra = compraService.registrarCompra(fornecedorId, itens)
        println("\nCompra #${compra.id} registrada! Total pago: ${Formatador.moeda(compra.total)} (movimentação #${compra.movimentacaoId})")
    }

    private fun listar() {
        println()
        val compras = compraService.listarCompras()
        if (compras.isEmpty()) println("Nenhuma compra registrada.")
        for (compra in compras) {
            println("Compra #${compra.id} - ${Formatador.dataHora(compra.dataHora)} - fornecedor: ${compra.fornecedorNome} - comprador: ${compra.compradorNome} - total: ${Formatador.moeda(compra.total)}")
            for (item in compra.itens) {
                println("     ${item.produtoNome} x ${item.quantidade} a ${Formatador.moeda(item.precoUnitario)} = ${Formatador.moeda(item.subtotal)}")
            }
        }
        Console.pausar()
    }
}
