package br.com.bikeshop.repository

import br.com.bikeshop.model.Cargo
import br.com.bikeshop.model.Cliente
import br.com.bikeshop.model.Empresa
import br.com.bikeshop.model.Fornecedor
import br.com.bikeshop.model.Funcionario
import br.com.bikeshop.model.Pessoa
import br.com.bikeshop.model.TipoPessoa
import java.sql.Connection
import java.sql.ResultSet

// SQL das tabelas pessoa e funcionario. Recebe a Connection do service para ficar na mesma transação.
class PessoaRepository {

    // LEFT JOIN porque cliente/fornecedor não têm linha em funcionario
    private val selectBase = """
        SELECT p.id, p.tipo, p.nome, p.documento, p.email, p.telefone, p.ativo,
               f.setor_id, f.cargo, f.salario
          FROM pessoa p
          LEFT JOIN funcionario f ON f.pessoa_id = p.id
    """

    fun inserir(conn: Connection, pessoa: Pessoa): Int {
        val sql = "INSERT INTO pessoa (tipo, nome, documento, email, telefone, ativo) VALUES (?, ?, ?, ?, ?, ?) RETURNING id"
        var id = 0
        conn.prepareStatement(sql).use { st ->
            st.setString(1, pessoa.tipo.name)
            st.setString(2, pessoa.nome)
            st.setString(3, pessoa.documento)
            st.setString(4, pessoa.email)      // se for null, grava NULL no banco
            st.setString(5, pessoa.telefone)
            st.setBoolean(6, pessoa.ativo)
            val rs = st.executeQuery()
            rs.next()
            id = rs.getInt("id")
        }

        // Se for funcionário, grava também a linha da tabela funcionario (mesmo id)
        if (pessoa is Funcionario) {
            val sqlFunc = "INSERT INTO funcionario (pessoa_id, setor_id, cargo, salario) VALUES (?, ?, ?, ?)"
            conn.prepareStatement(sqlFunc).use { st ->
                st.setInt(1, id)
                st.setInt(2, pessoa.setorId)
                st.setString(3, pessoa.cargo.name)
                st.setBigDecimal(4, pessoa.salario)
                st.executeUpdate()
            }
        }
        return id
    }

    fun buscarPorId(conn: Connection, id: Int): Pessoa? {
        conn.prepareStatement("$selectBase WHERE p.id = ?").use { st ->
            st.setInt(1, id)
            val rs = st.executeQuery()
            return if (rs.next()) mapear(rs) else null
        }
    }

    fun buscarPorDocumento(conn: Connection, documento: String): Pessoa? {
        conn.prepareStatement("$selectBase WHERE p.documento = ?").use { st ->
            st.setString(1, documento)
            val rs = st.executeQuery()
            return if (rs.next()) mapear(rs) else null
        }
    }

    fun listarPorTipo(conn: Connection, tipo: TipoPessoa): List<Pessoa> {
        val lista = mutableListOf<Pessoa>()
        conn.prepareStatement("$selectBase WHERE p.tipo = ? ORDER BY p.nome").use { st ->
            st.setString(1, tipo.name)
            val rs = st.executeQuery()
            while (rs.next()) {
                lista.add(mapear(rs))
            }
        }
        return lista
    }

    fun listarFuncionariosPorSetor(conn: Connection, setorId: Int): List<Funcionario> {
        val lista = mutableListOf<Funcionario>()
        conn.prepareStatement("$selectBase WHERE f.setor_id = ? ORDER BY p.nome").use { st ->
            st.setInt(1, setorId)
            val rs = st.executeQuery()
            while (rs.next()) {
                val pessoa = mapear(rs)
                if (pessoa is Funcionario) lista.add(pessoa)
            }
        }
        return lista
    }

    // Troca o setor de um funcionário. Só a coluna setor_id muda.
    fun mudarSetor(conn: Connection, funcionarioId: Int, novoSetorId: Int): Boolean {
        conn.prepareStatement("UPDATE funcionario SET setor_id = ? WHERE pessoa_id = ?").use { st ->
            st.setInt(1, novoSetorId)
            st.setInt(2, funcionarioId)
            return st.executeUpdate() == 1
        }
    }

    // Não apagamos pessoas (elas aparecem no histórico financeiro); só marcamos como inativas.
    fun desativar(conn: Connection, id: Int): Boolean {
        conn.prepareStatement("UPDATE pessoa SET ativo = FALSE WHERE id = ? AND id <> ?").use { st ->
            st.setInt(1, id)
            st.setInt(2, Empresa.ID) // a empresa nunca pode ser desativada
            return st.executeUpdate() == 1
        }
    }

    // Transforma uma linha do ResultSet no objeto certo, de acordo com a coluna "tipo"
    private fun mapear(rs: ResultSet): Pessoa {
        val id = rs.getInt("id")
        val nome = rs.getString("nome")
        val documento = rs.getString("documento")
        val email: String? = rs.getString("email")       // pode vir null do banco
        val telefone: String? = rs.getString("telefone")
        val ativo = rs.getBoolean("ativo")

        return when (TipoPessoa.valueOf(rs.getString("tipo"))) {
            TipoPessoa.CLIENTE -> Cliente(id, nome, documento, email, telefone, ativo)
            TipoPessoa.FORNECEDOR -> Fornecedor(id, nome, documento, email, telefone, ativo)
            TipoPessoa.EMPRESA -> Empresa(id, nome, documento, email, telefone, ativo)
            TipoPessoa.FUNCIONARIO -> Funcionario(
                id, nome, documento, email, telefone, ativo,
                setorId = rs.getInt("setor_id"),
                cargo = Cargo.valueOf(rs.getString("cargo")),
                salario = rs.getBigDecimal("salario")
            )
        }
    }
}
