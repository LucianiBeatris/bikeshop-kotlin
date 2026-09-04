package br.com.bikeshop.ui

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.Funcionario
import br.com.bikeshop.model.Setor
import br.com.bikeshop.repository.PessoaRepository
import br.com.bikeshop.repository.SetorRepository
import br.com.bikeshop.util.Console

// Setores e transferência de funcionários
class MenuSetores {

    private val setorRepo = SetorRepository()
    private val pessoaRepo = PessoaRepository()

    fun executar() {
        while (true) {
            println("\n---------- SETORES ----------")
            println("1 - Listar setores")
            println("2 - Cadastrar setor")
            println("3 - Listar funcionários de um setor")
            println("4 - Transferir funcionário de setor")
            println("0 - Voltar")
            val opcao = Console.lerInteiro("Opção", 0, 4)
            if (opcao == 0) return

            executarComSeguranca {
                when (opcao) {
                    1 -> listar()
                    2 -> cadastrar()
                    3 -> listarFuncionarios()
                    4 -> transferir()
                }
            }
        }
    }

    private fun listar() {
        println()
        Database.conectar().use { conn ->
            for (setor in setorRepo.listar(conn)) {
                val id = setor.id ?: continue
                val quantidade = setorRepo.contarFuncionarios(conn, id)
                println("#$id - ${setor.nome} (${setor.descricao ?: "sem descrição"}) - $quantidade funcionário(s)")
            }
        }
        Console.pausar()
    }

    private fun cadastrar() {
        println("\n-- Novo setor --")
        val nome = Console.lerTexto("Nome do setor")
        val descricao = Console.lerTextoOpcional("Descrição")
        val setor = Setor(null, nome, descricao)
        val erros = setor.validar()
        if (erros.isNotEmpty()) {
            erros.forEach { println("  - $it") }
            return
        }
        val id = Database.emTransacao { conn ->
            if (setorRepo.existeNome(conn, nome)) throw NegocioException("Já existe um setor chamado '$nome'.")
            setorRepo.inserir(conn, setor)
        }
        println("Setor cadastrado com id $id.")
    }

    private fun listarFuncionarios() {
        val setorId = Console.lerInteiro("Id do setor", 1)
        Database.conectar().use { conn ->
            val setor = setorRepo.buscarPorId(conn, setorId) ?: throw NegocioException("Setor $setorId não existe.")
            println("\nFuncionários do setor ${setor.nome}:")
            val funcionarios = pessoaRepo.listarFuncionariosPorSetor(conn, setorId)
            if (funcionarios.isEmpty()) println("  (nenhum)")
            for (f in funcionarios) {
                println("  $f")
            }
        }
        Console.pausar()
    }

    // Mudar de setor = atualizar só a coluna funcionario.setor_id
    private fun transferir() {
        val funcionarioId = Console.lerInteiro("Id do funcionário", 1)
        val funcionario = Database.conectar().use { conn -> pessoaRepo.buscarPorId(conn, funcionarioId) } as? Funcionario
            ?: throw NegocioException("Funcionário $funcionarioId não encontrado.")
        println("Funcionário: ${funcionario.nome} (setor atual: ${funcionario.setorId})")

        val novoSetorId = Console.lerInteiro("Id do novo setor", 1)
        Database.emTransacao { conn ->
            val setor = setorRepo.buscarPorId(conn, novoSetorId) ?: throw NegocioException("Setor $novoSetorId não existe.")
            pessoaRepo.mudarSetor(conn, funcionarioId, novoSetorId)
            println("${funcionario.nome} transferido(a) para o setor ${setor.nome}.")
        }
    }
}
