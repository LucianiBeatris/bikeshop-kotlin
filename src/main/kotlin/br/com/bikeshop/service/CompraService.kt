package br.com.bikeshop.service

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.Compra
import br.com.bikeshop.model.Fornecedor
import br.com.bikeshop.model.ItemOperacao
import br.com.bikeshop.model.OrigemMovimentacao
import br.com.bikeshop.repository.CompraRepository
import br.com.bikeshop.repository.PessoaRepository
import br.com.bikeshop.repository.ProdutoRepository

// Compra: paga o fornecedor (saída do caixa), grava a compra e aumenta o estoque
class CompraService {

    private val caixa = Caixa()
    private val pessoaRepo = PessoaRepository()
    private val produtoRepo = ProdutoRepository()
    private val compraRepo = CompraRepository()

    fun registrarCompra(fornecedorId: Int, itens: List<ItemOperacao>): Compra {
        if (itens.isEmpty()) throw NegocioException("A compra precisa ter pelo menos um item.")
        val comprador = Sessao.funcionarioLogado ?: throw NegocioException("Nenhum funcionário logado.")
        val compradorId = comprador.id ?: throw NegocioException("Funcionário logado sem id.")

        return Database.emTransacao { conn ->
            val fornecedor = pessoaRepo.buscarPorId(conn, fornecedorId) as? Fornecedor
                ?: throw NegocioException("Fornecedor $fornecedorId não encontrado.")
            if (!fornecedor.ativo) throw NegocioException("Fornecedor está inativo.")

            for (item in itens) {
                if (produtoRepo.buscarPorId(conn, item.produtoId) == null) {
                    throw NegocioException("Produto ${item.produtoId} não encontrado.")
                }
            }
            val total = itens.sumOf { it.subtotal }

            // 1) paga o fornecedor. Se não tiver saldo, lança SaldoInsuficienteException e nada é gravado
            val movimentacao = caixa.registrarSaida(conn, total, fornecedorId, OrigemMovimentacao.COMPRA, "Compra de ${fornecedor.nome}")

            // 2) grava a compra e os itens
            val compra = Compra(null, fornecedorId, fornecedor.nome, compradorId, comprador.nome, movimentacao.id, null, total, itens)
            val compraId = compraRepo.inserir(conn, compra)
            for (item in itens) {
                compraRepo.inserirItem(conn, compraId, item)
                // 3) a mercadoria entra no estoque
                produtoRepo.adicionarEstoque(conn, item.produtoId, item.quantidade)
            }
            Compra(compraId, fornecedorId, fornecedor.nome, compradorId, comprador.nome, movimentacao.id, null, total, itens)
        }
    }

    fun listarCompras(): List<Compra> {
        Database.conectar().use { conn ->
            return compraRepo.listar(conn)
        }
    }
}
