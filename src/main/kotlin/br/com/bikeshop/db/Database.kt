package br.com.bikeshop.db

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

// Conexão com o PostgreSQL. Usa variáveis de ambiente ou os valores padrão.
object Database {

    private val url = System.getenv("BIKESHOP_DB_URL") ?: "jdbc:postgresql://localhost:5432/bikeshop"
    private val usuario = System.getenv("BIKESHOP_DB_USER") ?: "postgres"
    private val senha = System.getenv("BIKESHOP_DB_PASSWORD") ?: "postgres"

    // Abre uma conexão nova com o banco. Quem chama é responsável por fechar (use { }).
    fun conectar(): Connection {
        return DriverManager.getConnection(url, usuario, senha)
    }

    // Roda o bloco dentro de uma transação: deu certo -> commit, deu erro -> rollback
    fun <T> emTransacao(bloco: (Connection) -> T): T {
        val conn = conectar()
        try {
            conn.autoCommit = false
            val resultado = bloco(conn)
            conn.commit()
            return resultado
        } catch (e: Exception) {
            conn.rollback()
            throw e
        } finally {
            conn.close()
        }
    }

    // Cria o banco (se não existir) e roda o schema.sql, que cria as tabelas e os dados iniciais.
    fun inicializar() {
        criarBancoSeNaoExistir()
        val sql = Database::class.java.getResource("/schema.sql")?.readText()
            ?: throw IllegalStateException("Arquivo schema.sql não encontrado em resources")
        conectar().use { conn ->
            conn.createStatement().use { st ->
                st.execute(sql)
            }
        }
    }

    // Tenta conectar; se o erro for "banco não existe" (código 3D000), conecta no banco padrão e cria.
    private fun criarBancoSeNaoExistir() {
        try {
            conectar().close()
        } catch (e: SQLException) {
            if (e.sqlState != "3D000") throw e
            val nomeBanco = url.substringAfterLast("/")
            val urlPadrao = url.substringBeforeLast("/") + "/postgres"
            DriverManager.getConnection(urlPadrao, usuario, senha).use { conn ->
                conn.createStatement().use { st ->
                    st.execute("CREATE DATABASE $nomeBanco")
                }
            }
            println("Banco de dados '$nomeBanco' criado.")
        }
    }
}
