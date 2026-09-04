package br.com.bikeshop.repository

import br.com.bikeshop.model.OrdemServico
import br.com.bikeshop.model.StatusOrdemServico
import java.sql.Connection
import java.sql.ResultSet

// SQL da tabela ordem_servico
class OrdemServicoRepository {

    private val selectBase = """
        SELECT os.id, os.cliente_id, c.nome AS cliente_nome, os.mecanico_id, m.nome AS mecanico_nome,
               os.descricao, os.valor, os.status, os.data_abertura, os.data_conclusao, os.movimentacao_id
          FROM ordem_servico os
          JOIN pessoa c ON c.id = os.cliente_id
          JOIN pessoa m ON m.id = os.mecanico_id
    """

    fun inserir(conn: Connection, os: OrdemServico): Int {
        val sql = "INSERT INTO ordem_servico (cliente_id, mecanico_id, descricao, valor, status) VALUES (?, ?, ?, ?, ?) RETURNING id"
        conn.prepareStatement(sql).use { st ->
            st.setInt(1, os.clienteId)
            st.setInt(2, os.mecanicoId)
            st.setString(3, os.descricao)
            st.setBigDecimal(4, os.valor)
            st.setString(5, os.status.name)
            val rs = st.executeQuery()
            rs.next()
            return rs.getInt("id")
        }
    }

    fun listar(conn: Connection): List<OrdemServico> {
        val lista = mutableListOf<OrdemServico>()
        conn.prepareStatement("$selectBase ORDER BY os.id DESC").use { st ->
            val rs = st.executeQuery()
            while (rs.next()) {
                lista.add(mapear(rs))
            }
        }
        return lista
    }

    fun buscarPorId(conn: Connection, id: Int): OrdemServico? {
        conn.prepareStatement("$selectBase WHERE os.id = ?").use { st ->
            st.setInt(1, id)
            val rs = st.executeQuery()
            return if (rs.next()) mapear(rs) else null
        }
    }

    fun atualizarStatus(conn: Connection, id: Int, status: StatusOrdemServico): Boolean {
        conn.prepareStatement("UPDATE ordem_servico SET status = ? WHERE id = ?").use { st ->
            st.setString(1, status.name)
            st.setInt(2, id)
            return st.executeUpdate() == 1
        }
    }

    // Ao concluir, preenche os campos que estavam NULL: data_conclusao e movimentacao_id
    fun concluir(conn: Connection, id: Int, movimentacaoId: Int): Boolean {
        val sql = "UPDATE ordem_servico SET status = 'CONCLUIDA', data_conclusao = CURRENT_TIMESTAMP, movimentacao_id = ? WHERE id = ?"
        conn.prepareStatement(sql).use { st ->
            st.setInt(1, movimentacaoId)
            st.setInt(2, id)
            return st.executeUpdate() == 1
        }
    }

    private fun mapear(rs: ResultSet): OrdemServico {
        // getTimestamp pode devolver null (OS ainda não concluída), por isso o "?."
        val conclusao = rs.getTimestamp("data_conclusao")?.toLocalDateTime()
        // getInt devolve 0 quando a coluna é NULL; rs.wasNull() diz se era NULL mesmo
        val movId = rs.getInt("movimentacao_id")
        val movimentacaoId: Int? = if (rs.wasNull()) null else movId

        return OrdemServico(
            id = rs.getInt("id"),
            clienteId = rs.getInt("cliente_id"),
            clienteNome = rs.getString("cliente_nome"),
            mecanicoId = rs.getInt("mecanico_id"),
            mecanicoNome = rs.getString("mecanico_nome"),
            descricao = rs.getString("descricao"),
            valor = rs.getBigDecimal("valor"),
            status = StatusOrdemServico.valueOf(rs.getString("status")),
            dataAbertura = rs.getTimestamp("data_abertura").toLocalDateTime(),
            dataConclusao = conclusao,
            movimentacaoId = movimentacaoId
        )
    }
}
