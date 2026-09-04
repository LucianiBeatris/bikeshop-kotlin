package br.com.bikeshop.repository

import br.com.bikeshop.model.Setor
import java.sql.Connection

// SQL da tabela setor
class SetorRepository {

    fun inserir(conn: Connection, setor: Setor): Int {
        conn.prepareStatement("INSERT INTO setor (nome, descricao) VALUES (?, ?) RETURNING id").use { st ->
            st.setString(1, setor.nome)
            st.setString(2, setor.descricao)
            val rs = st.executeQuery()
            rs.next()
            return rs.getInt("id")
        }
    }

    fun listar(conn: Connection): List<Setor> {
        val lista = mutableListOf<Setor>()
        conn.prepareStatement("SELECT id, nome, descricao FROM setor ORDER BY id").use { st ->
            val rs = st.executeQuery()
            while (rs.next()) {
                lista.add(Setor(rs.getInt("id"), rs.getString("nome"), rs.getString("descricao")))
            }
        }
        return lista
    }

    fun buscarPorId(conn: Connection, id: Int): Setor? {
        conn.prepareStatement("SELECT id, nome, descricao FROM setor WHERE id = ?").use { st ->
            st.setInt(1, id)
            val rs = st.executeQuery()
            return if (rs.next()) Setor(rs.getInt("id"), rs.getString("nome"), rs.getString("descricao")) else null
        }
    }

    fun existeNome(conn: Connection, nome: String): Boolean {
        conn.prepareStatement("SELECT 1 FROM setor WHERE LOWER(nome) = LOWER(?)").use { st ->
            st.setString(1, nome)
            return st.executeQuery().next()
        }
    }

    fun contarFuncionarios(conn: Connection, setorId: Int): Int {
        conn.prepareStatement("SELECT COUNT(*) FROM funcionario WHERE setor_id = ?").use { st ->
            st.setInt(1, setorId)
            val rs = st.executeQuery()
            rs.next()
            return rs.getInt(1)
        }
    }
}
