package br.com.bikeshop.ui

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.OperacaoCanceladaException
import br.com.bikeshop.model.Funcionario
import br.com.bikeshop.repository.PessoaRepository
import br.com.bikeshop.service.Sessao
import br.com.bikeshop.util.Console
import br.com.bikeshop.util.Validador

// Login pelo CPF do funcionário
object TelaLogin {

    private val pessoaRepo = PessoaRepository()

    // Devolve true se alguém logou; false se o usuário desistiu (digitou "cancelar")
    fun autenticar(): Boolean {
        println("\n========== LOGIN ==========")
        println("Digite o CPF do funcionário (administrador padrão: 123.456.789-09) ou 'cancelar' para sair.")
        while (true) {
            try {
                val cpf = Console.lerTexto("CPF", Validador.REGEX_CPF, "CPF deve estar no formato 000.000.000-00")
                val pessoa = Database.conectar().use { conn -> pessoaRepo.buscarPorDocumento(conn, cpf) }
                if (pessoa is Funcionario && pessoa.ativo) {
                    Sessao.funcionarioLogado = pessoa
                    println("Bem-vindo(a), ${pessoa.nome} (${pessoa.cargo})!")
                    return true
                }
                println("  ! Nenhum funcionário ativo encontrado com esse CPF.")
            } catch (e: OperacaoCanceladaException) {
                return false
            }
        }
    }
}
