package br.com.bikeshop.repository

import br.com.bikeshop.model.Compra
import br.com.bikeshop.model.ItemOperacao
import java.sql.Connection

// SQL das tabelas compra e item_compra (igual ao VendaRepository)
class CompraRepository {

    fun inserir(conn: Connection, compra: Compra): Int {
        val sql = "INSERT INTO compra (fornecedor_id, comprador_id, movimentacao_id, total) VALUES (?, ?, ?, ?) RETURNING id"
        conn.prepareStatement(sql).use { st ->
            st.setInt(1, compra.fornecedorId)
            st.setInt(2, compra.compradorId)
            st.setInt(3, compra.movimentacaoId ?: throw IllegalStateException("Compra sem movimentação financeira"))
            st.setBigDecimal(4, compra.total)
            val rs = st.executeQuery()
            rs.next()
            return rs.getInt("id")
        }
    }

    fun inserirItem(conn: Connection, compraId: Int, item: ItemOperacao) {
        val sql = "INSERT INTO item_compra (compra_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)"
        conn.prepareStatement(sql).use { st ->
            st.setInt(1, compraId)
            st.setInt(2, item.produtoId)
            st.setInt(3, item.quantidade)
            st.setBigDecimal(4, item.precoUnitario)
            st.executeUpdate()
        }
    }

    fun listar(conn: Connection): List<Compra> {
        val lista = mutableListOf<Compra>()
        val sql = """
            SELECT c.id, c.fornecedor_id, fo.nome AS fornecedor_nome, c.comprador_id, fu.nome AS comprador_nome,
                   c.movimentacao_id, c.data_hora, c.total
              FROM compra c
              JOIN pessoa fo ON fo.id = c.fornecedor_id
              JOIN pessoa fu ON fu.id = c.comprador_id
             ORDER BY c.data_hora DESC
        """
        conn.prepareStatement(sql).use { st ->
            val rs = st.executeQuery()
            while (rs.next()) {
                val compraId = rs.getInt("id")
                lista.add(
                    Compra(
                        id = compraId,
                        fornecedorId = rs.getInt("fornecedor_id"),
                        fornecedorNome = rs.getString("fornecedor_nome"),
                        compradorId = rs.getInt("comprador_id"),
                        compradorNome = rs.getString("comprador_nome"),
                        movimentacaoId = rs.getInt("movimentacao_id"),
                        dataHora = rs.getTimestamp("data_hora").toLocalDateTime(),
                        total = rs.getBigDecimal("total"),
                        itens = listarItens(conn, compraId)
                    )
                )
            }
        }
        return lista
    }

    fun listarItens(conn: Connection, compraId: Int): List<ItemOperacao> {
        val lista = mutableListOf<ItemOperacao>()
        val sql = """
            SELECT i.produto_id, p.nome, i.quantidade, i.preco_unitario
              FROM item_compra i
              JOIN produto p ON p.id = i.produto_id
             WHERE i.compra_id = ?
        """
        conn.prepareStatement(sql).use { st ->
            st.setInt(1, compraId)
            val rs = st.executeQuery()
            while (rs.next()) {
                lista.add(ItemOperacao(rs.getInt("produto_id"), rs.getString("nome"), rs.getInt("quantidade"), rs.getBigDecimal("preco_unitario")))
            }
        }
        return lista
    }
}
