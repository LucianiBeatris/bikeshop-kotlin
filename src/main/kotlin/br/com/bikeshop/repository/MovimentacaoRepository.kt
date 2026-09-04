package br.com.bikeshop.repository

import br.com.bikeshop.model.MovimentacaoFinanceira
import br.com.bikeshop.model.OrigemMovimentacao
import br.com.bikeshop.model.TipoMovimentacao
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp

// SQL das tabelas movimentacao_financeira e caixa. Só o Caixa (service) usa esta classe.
class MovimentacaoRepository {

    // JOIN três vezes com pessoa para trazer os nomes de quem pagou, quem recebeu e quem foi o responsável
    private val selectBase = """
        SELECT m.id, m.tipo, m.origem, m.valor, m.pagador_id, m.recebedor_id, m.responsavel_id, m.data_hora, m.descricao,
               pg.nome AS pagador_nome, rc.nome AS recebedor_nome, rp.nome AS responsavel_nome
          FROM movimentacao_financeira m
          JOIN pessoa pg ON pg.id = m.pagador_id
          JOIN pessoa rc ON rc.id = m.recebedor_id
          JOIN pessoa rp ON rp.id = m.responsavel_id
    """

    fun lerSaldo(conn: Connection): BigDecimal {
        conn.prepareStatement("SELECT saldo FROM caixa WHERE id = 1").use { st ->
            val rs = st.executeQuery()
            rs.next()
            return rs.getBigDecimal("saldo")
        }
    }

    // Soma a variação no saldo. Se ficaria negativo, o WHERE não deixa e devolve null.
    fun atualizarSaldo(conn: Connection, variacao: BigDecimal): BigDecimal? {
        val sql = "UPDATE caixa SET saldo = saldo + ? WHERE id = 1 AND saldo + ? >= 0 RETURNING saldo"
        conn.prepareStatement(sql).use { st ->
            st.setBigDecimal(1, variacao)
            st.setBigDecimal(2, variacao)
            val rs = st.executeQuery()
            return if (rs.next()) rs.getBigDecimal("saldo") else null
        }
    }

    // AQUI a movimentação financeira é gravada no banco.
    fun inserir(conn: Connection, mov: MovimentacaoFinanceira): Int {
        val sql = """
            INSERT INTO movimentacao_financeira
                (tipo, origem, valor, pagador_id, recebedor_id, responsavel_id, data_hora, descricao)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
        """
        conn.prepareStatement(sql).use { st ->
            st.setString(1, mov.tipo.name)
            st.setString(2, mov.origem.name)
            st.setBigDecimal(3, mov.valor)
            st.setInt(4, mov.pagadorId)
            st.setInt(5, mov.recebedorId)
            st.setInt(6, mov.responsavelId)
            st.setTimestamp(7, Timestamp.valueOf(mov.dataHora))
            st.setString(8, mov.descricao)
            val rs = st.executeQuery()
            rs.next()
            return rs.getInt("id")
        }
    }

    // Extrato. Se tipo for null, lista tudo; se não, só ENTRADA ou só SAIDA.
    fun listar(conn: Connection, tipo: TipoMovimentacao? = null): List<MovimentacaoFinanceira> {
        val lista = mutableListOf<MovimentacaoFinanceira>()
        val sql = if (tipo == null) "$selectBase ORDER BY m.data_hora DESC" else "$selectBase WHERE m.tipo = ? ORDER BY m.data_hora DESC"
        conn.prepareStatement(sql).use { st ->
            if (tipo != null) st.setString(1, tipo.name)
            val rs = st.executeQuery()
            while (rs.next()) {
                lista.add(mapear(rs))
            }
        }
        return lista
    }

    // Auditoria: tudo que um determinado funcionário registrou
    fun listarPorResponsavel(conn: Connection, responsavelId: Int): List<MovimentacaoFinanceira> {
        val lista = mutableListOf<MovimentacaoFinanceira>()
        conn.prepareStatement("$selectBase WHERE m.responsavel_id = ? ORDER BY m.data_hora DESC").use { st ->
            st.setInt(1, responsavelId)
            val rs = st.executeQuery()
            while (rs.next()) {
                lista.add(mapear(rs))
            }
        }
        return lista
    }

    private fun mapear(rs: ResultSet): MovimentacaoFinanceira {
        return MovimentacaoFinanceira(
            id = rs.getInt("id"),
            tipo = TipoMovimentacao.valueOf(rs.getString("tipo")),
            origem = OrigemMovimentacao.valueOf(rs.getString("origem")),
            valor = rs.getBigDecimal("valor"),
            pagadorId = rs.getInt("pagador_id"),
            recebedorId = rs.getInt("recebedor_id"),
            responsavelId = rs.getInt("responsavel_id"),
            dataHora = rs.getTimestamp("data_hora").toLocalDateTime(),
            descricao = rs.getString("descricao"),
            pagadorNome = rs.getString("pagador_nome"),
            recebedorNome = rs.getString("recebedor_nome"),
            responsavelNome = rs.getString("responsavel_nome")
        )
    }
}
