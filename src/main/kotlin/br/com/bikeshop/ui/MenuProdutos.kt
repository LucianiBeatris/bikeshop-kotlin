package br.com.bikeshop.ui

import br.com.bikeshop.db.Database
import br.com.bikeshop.model.Produto
import br.com.bikeshop.repository.ProdutoRepository
import br.com.bikeshop.util.Console

// Produtos e estoque
class MenuProdutos {

    private val produtoRepo = ProdutoRepository()

    fun executar() {
        while (true) {
            println("\n---------- PRODUTOS / ESTOQUE ----------")
            println("1 - Cadastrar produto")
            println("2 - Listar produtos e estoque")
            println("3 - Produtos com estoque baixo (menos de 5 unidades)")
            println("0 - Voltar")
            val opcao = Console.lerInteiro("Opção", 0, 3)
            if (opcao == 0) return

            executarComSeguranca {
                when (opcao) {
                    1 -> cadastrar()
                    2 -> listar()
                    3 -> estoqueBaixo()
                }
            }
        }
    }

    private fun cadastrar() {
        println("\n-- Novo produto (digite 'cancelar' para desistir) --")
        val nome = Console.lerTexto("Nome")
        val descricao = Console.lerTextoOpcional("Descrição")
        val precoCusto = Console.lerDecimal("Preço de custo")
        val precoVenda = Console.lerDecimal("Preço de venda")
        val estoque = Console.lerInteiro("Quantidade inicial em estoque", 0)

        val produto = Produto(null, nome, descricao, precoCusto, precoVenda, estoque)
        val erros = produto.validar()
        if (erros.isNotEmpty()) {
            println("\nCadastro NÃO realizado:")
            erros.forEach { println("  - $it") }
            return
        }
        val id = Database.emTransacao { conn -> produtoRepo.inserir(conn, produto) }
        println("Produto cadastrado com id $id.")
    }

    private fun listar() {
        println()
        val produtos = Database.conectar().use { conn -> produtoRepo.listar(conn) }
        if (produtos.isEmpty()) println("Nenhum produto cadastrado.")
        for (produto in produtos) {
            println(produto)
        }
        Console.pausar()
    }

    private fun estoqueBaixo() {
        println()
        val produtos = Database.conectar().use { conn -> produtoRepo.listar(conn) }
        val baixos = produtos.filter { it.quantidadeEstoque < 5 }
        if (baixos.isEmpty()) println("Nenhum produto com estoque baixo.")
        for (produto in baixos) {
            println(produto)
        }
        Console.pausar()
    }
}
