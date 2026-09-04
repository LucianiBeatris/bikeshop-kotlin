package br.com.bikeshop.service

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.EstoqueInsuficienteException
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.Cliente
import br.com.bikeshop.model.ItemOperacao
import br.com.bikeshop.model.OrigemMovimentacao
import br.com.bikeshop.model.Venda
import br.com.bikeshop.repository.PessoaRepository
import br.com.bikeshop.repository.ProdutoRepository
import br.com.bikeshop.repository.VendaRepository

// Venda: baixa o estoque, registra a entrada no caixa e grava a venda
class VendaService {

    private val caixa = Caixa()
    private val pessoaRepo = PessoaRepository()
    private val produtoRepo = ProdutoRepository()
    private val vendaRepo = VendaRepository()

    // pedidos = lista de (produtoId, quantidade). Tudo dentro de uma transação só.
    fun registrarVenda(clienteId: Int, pedidos: List<Pair<Int, Int>>): Venda {
        if (pedidos.isEmpty()) throw NegocioException("A venda precisa ter pelo menos um item.")
        val vendedor = Sessao.funcionarioLogado ?: throw NegocioException("Nenhum funcionário logado.")
        val vendedorId = vendedor.id ?: throw NegocioException("Funcionário logado sem id.")

        return Database.emTransacao { conn ->
            // "as?" tenta converter para Cliente; se a pessoa não for cliente vira null e cai no elvis
            val cliente = pessoaRepo.buscarPorId(conn, clienteId) as? Cliente
                ?: throw NegocioException("Cliente $clienteId não encontrado.")
            if (!cliente.ativo) throw NegocioException("Cliente está inativo.")

            // Se o mesmo produto foi digitado duas vezes, soma as quantidades
            val agrupado = mutableMapOf<Int, Int>()
            for ((produtoId, quantidade) in pedidos) {
                agrupado[produtoId] = (agrupado[produtoId] ?: 0) + quantidade
            }

            // 1) baixa o estoque de cada produto (o preço é o preço de venda atual do cadastro)
            val itens = mutableListOf<ItemOperacao>()
            for ((produtoId, quantidade) in agrupado) {
                val produto = produtoRepo.buscarPorId(conn, produtoId)
                    ?: throw NegocioException("Produto $produtoId não encontrado.")
                if (!produtoRepo.baixarEstoque(conn, produtoId, quantidade)) {
                    throw EstoqueInsuficienteException(produto.nome, quantidade, produto.quantidadeEstoque)
                }
                itens.add(ItemOperacao(produtoId, produto.nome, quantidade, produto.precoVenda))
            }
            val total = itens.sumOf { it.subtotal }

            // 2) o dinheiro entra no caixa (gera a movimentação financeira; cliente paga, empresa recebe)
            val movimentacao = caixa.registrarEntrada(conn, total, clienteId, OrigemMovimentacao.VENDA, "Venda para ${cliente.nome}")

            // 3) grava a venda apontando para a movimentação, e depois os itens (N para N)
            val venda = Venda(null, clienteId, cliente.nome, vendedorId, vendedor.nome, movimentacao.id, null, total, itens)
            val vendaId = vendaRepo.inserir(conn, venda)
            for (item in itens) {
                vendaRepo.inserirItem(conn, vendaId, item)
            }
            Venda(vendaId, clienteId, cliente.nome, vendedorId, vendedor.nome, movimentacao.id, null, total, itens)
        }
    }

    fun listarVendas(): List<Venda> {
        Database.conectar().use { conn ->
            return vendaRepo.listar(conn)
        }
    }
}
