package br.com.bikeshop.repository

import br.com.bikeshop.model.ItemOperacao
import br.com.bikeshop.model.Venda
import java.sql.Connection

// SQL das tabelas venda e item_venda
class VendaRepository {

    fun inserir(conn: Connection, venda: Venda): Int {
        val sql = "INSERT INTO venda (cliente_id, vendedor_id, movimentacao_id, total) VALUES (?, ?, ?, ?) RETURNING id"
        conn.prepareStatement(sql).use { st ->
            st.setInt(1, venda.clienteId)
            st.setInt(2, venda.vendedorId)
            st.setInt(3, venda.movimentacaoId ?: throw IllegalStateException("Venda sem movimentação financeira"))
            st.setBigDecimal(4, venda.total)
            val rs = st.executeQuery()
            rs.next()
            return rs.getInt("id")
        }
    }

    // Grava a ligação N para N: cada linha é um produto dentro de uma venda
    fun inserirItem(conn: Connection, vendaId: Int, item: ItemOperacao) {
        val sql = "INSERT INTO item_venda (venda_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)"
        conn.prepareStatement(sql).use { st ->
            st.setInt(1, vendaId)
            st.setInt(2, item.produtoId)
            st.setInt(3, item.quantidade)
            st.setBigDecimal(4, item.precoUnitario)
            st.executeUpdate()
        }
    }

    fun listar(conn: Connection): List<Venda> {
        val lista = mutableListOf<Venda>()
        val sql = """
            SELECT v.id, v.cliente_id, c.nome AS cliente_nome, v.vendedor_id, f.nome AS vendedor_nome,
                   v.movimentacao_id, v.data_hora, v.total
              FROM venda v
              JOIN pessoa c ON c.id = v.cliente_id
              JOIN pessoa f ON f.id = v.vendedor_id
             ORDER BY v.data_hora DESC
        """
        conn.prepareStatement(sql).use { st ->
            val rs = st.executeQuery()
            while (rs.next()) {
                val vendaId = rs.getInt("id")
                lista.add(
                    Venda(
                        id = vendaId,
                        clienteId = rs.getInt("cliente_id"),
                        clienteNome = rs.getString("cliente_nome"),
                        vendedorId = rs.getInt("vendedor_id"),
                        vendedorNome = rs.getString("vendedor_nome"),
                        movimentacaoId = rs.getInt("movimentacao_id"),
                        dataHora = rs.getTimestamp("data_hora").toLocalDateTime(),
                        total = rs.getBigDecimal("total"),
                        itens = listarItens(conn, vendaId)
                    )
                )
            }
        }
        return lista
    }

    fun listarItens(conn: Connection, vendaId: Int): List<ItemOperacao> {
        val lista = mutableListOf<ItemOperacao>()
        val sql = """
            SELECT i.produto_id, p.nome, i.quantidade, i.preco_unitario
              FROM item_venda i
              JOIN produto p ON p.id = i.produto_id
             WHERE i.venda_id = ?
        """
        conn.prepareStatement(sql).use { st ->
            st.setInt(1, vendaId)
            val rs = st.executeQuery()
            while (rs.next()) {
                lista.add(ItemOperacao(rs.getInt("produto_id"), rs.getString("nome"), rs.getInt("quantidade"), rs.getBigDecimal("preco_unitario")))
            }
        }
        return lista
    }
}
