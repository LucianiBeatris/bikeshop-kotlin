package br.com.bikeshop.repository

import br.com.bikeshop.model.Produto
import java.sql.Connection
import java.sql.ResultSet

// SQL da tabela produto (cadastro e estoque)
class ProdutoRepository {

    fun inserir(conn: Connection, produto: Produto): Int {
        val sql = "INSERT INTO produto (nome, descricao, preco_custo, preco_venda, quantidade_estoque, ativo) VALUES (?, ?, ?, ?, ?, ?) RETURNING id"
        conn.prepareStatement(sql).use { st ->
            st.setString(1, produto.nome)
            st.setString(2, produto.descricao)
            st.setBigDecimal(3, produto.precoCusto)
            st.setBigDecimal(4, produto.precoVenda)
            st.setInt(5, produto.quantidadeEstoque)
            st.setBoolean(6, produto.ativo)
            val rs = st.executeQuery()
            rs.next()
            return rs.getInt("id")
        }
    }

    fun listar(conn: Connection): List<Produto> {
        val lista = mutableListOf<Produto>()
        conn.prepareStatement("SELECT * FROM produto WHERE ativo = TRUE ORDER BY nome").use { st ->
            val rs = st.executeQuery()
            while (rs.next()) {
                lista.add(mapear(rs))
            }
        }
        return lista
    }

    fun buscarPorId(conn: Connection, id: Int): Produto? {
        conn.prepareStatement("SELECT * FROM produto WHERE id = ? AND ativo = TRUE").use { st ->
            st.setInt(1, id)
            val rs = st.executeQuery()
            return if (rs.next()) mapear(rs) else null
        }
    }

    // Baixa o estoque. O WHERE confere se tem quantidade; se não tiver, não altera nada e devolve false.
    fun baixarEstoque(conn: Connection, produtoId: Int, quantidade: Int): Boolean {
        val sql = "UPDATE produto SET quantidade_estoque = quantidade_estoque - ? WHERE id = ? AND quantidade_estoque >= ?"
        conn.prepareStatement(sql).use { st ->
            st.setInt(1, quantidade)
            st.setInt(2, produtoId)
            st.setInt(3, quantidade)
            return st.executeUpdate() == 1
        }
    }

    fun adicionarEstoque(conn: Connection, produtoId: Int, quantidade: Int): Boolean {
        conn.prepareStatement("UPDATE produto SET quantidade_estoque = quantidade_estoque + ? WHERE id = ?").use { st ->
            st.setInt(1, quantidade)
            st.setInt(2, produtoId)
            return st.executeUpdate() == 1
        }
    }

    private fun mapear(rs: ResultSet): Produto {
        return Produto(
            id = rs.getInt("id"),
            nome = rs.getString("nome"),
            descricao = rs.getString("descricao"),
            precoCusto = rs.getBigDecimal("preco_custo"),
            precoVenda = rs.getBigDecimal("preco_venda"),
            quantidadeEstoque = rs.getInt("quantidade_estoque"),
            ativo = rs.getBoolean("ativo")
        )
    }
}
