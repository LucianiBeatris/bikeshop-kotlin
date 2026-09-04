package br.com.bikeshop.ui

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.TipoPessoa
import br.com.bikeshop.repository.PessoaRepository
import br.com.bikeshop.repository.ProdutoRepository
import br.com.bikeshop.service.VendaService
import br.com.bikeshop.util.Console
import br.com.bikeshop.util.Formatador

// Vendas
class MenuVendas {

    private val vendaService = VendaService()
    private val pessoaRepo = PessoaRepository()
    private val produtoRepo = ProdutoRepository()

    fun executar() {
        while (true) {
            println("\n---------- VENDAS ----------")
            println("1 - Registrar venda")
            println("2 - Listar vendas")
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
        println("\n-- Nova venda (digite 'cancelar' para desistir) --")

        // 1) escolhe o cliente
        val clientes = Database.conectar().use { conn -> pessoaRepo.listarPorTipo(conn, TipoPessoa.CLIENTE) }
        if (clientes.isEmpty()) throw NegocioException("Nenhum cliente cadastrado. Cadastre um cliente primeiro.")
        println("Clientes:")
        clientes.filter { it.ativo }.forEach { println("  $it") }
        val clienteId = Console.lerInteiro("Id do cliente", 1)

        // 2) monta o carrinho: produto + quantidade, até digitar 0
        val produtos = Database.conectar().use { conn -> produtoRepo.listar(conn) }
        println("Produtos:")
        produtos.forEach { println("  $it") }

        val pedidos = mutableListOf<Pair<Int, Int>>()
        while (true) {
            val produtoId = Console.lerInteiro("Id do produto (0 para fechar o carrinho)", 0)
            if (produtoId == 0) break
            val produto = produtos.find { it.id == produtoId }
            if (produto == null) {
                println("  ! Produto $produtoId não existe.")
                continue
            }
            val quantidade = Console.lerInteiro("Quantidade", 1)
            if (!produto.temEstoque(quantidade)) {
                println("  ! Estoque insuficiente (disponível: ${produto.quantidadeEstoque}).")
                continue
            }
            pedidos.add(Pair(produtoId, quantidade))
            println("  + ${produto.nome} x $quantidade = ${Formatador.moeda(produto.precoVenda.multiply(quantidade.toBigDecimal()))}")
        }
        if (pedidos.isEmpty()) throw NegocioException("Carrinho vazio, venda não registrada.")

        if (!Console.confirmar("Confirmar a venda?")) throw NegocioException("Venda não confirmada.")

        // 3) o serviço faz tudo dentro de uma transação
        val venda = vendaService.registrarVenda(clienteId, pedidos)
        println("\nVenda #${venda.id} registrada! Total: ${Formatador.moeda(venda.total)} (movimentação #${venda.movimentacaoId})")
    }

    private fun listar() {
        println()
        val vendas = vendaService.listarVendas()
        if (vendas.isEmpty()) println("Nenhuma venda registrada.")
        for (venda in vendas) {
            println("Venda #${venda.id} - ${Formatador.dataHora(venda.dataHora)} - cliente: ${venda.clienteNome} - vendedor: ${venda.vendedorNome} - total: ${Formatador.moeda(venda.total)}")
            for (item in venda.itens) {
                println("     ${item.produtoNome} x ${item.quantidade} a ${Formatador.moeda(item.precoUnitario)} = ${Formatador.moeda(item.subtotal)}")
            }
        }
        Console.pausar()
    }
}
