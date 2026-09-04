package br.com.bikeshop.ui

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.Cargo
import br.com.bikeshop.model.Cliente
import br.com.bikeshop.model.Empresa
import br.com.bikeshop.model.Fornecedor
import br.com.bikeshop.model.Funcionario
import br.com.bikeshop.model.Pessoa
import br.com.bikeshop.model.TipoPessoa
import br.com.bikeshop.repository.PessoaRepository
import br.com.bikeshop.repository.SetorRepository
import br.com.bikeshop.util.Console
import br.com.bikeshop.util.Validador
import java.math.BigDecimal

// Cadastro e listagem de clientes, fornecedores e funcionários
class MenuPessoas {

    private val pessoaRepo = PessoaRepository()
    private val setorRepo = SetorRepository()

    fun executar() {
        while (true) {
            println("\n---------- PESSOAS ----------")
            println("1 - Cadastrar cliente")
            println("2 - Cadastrar fornecedor")
            println("3 - Cadastrar funcionário")
            println("4 - Listar clientes")
            println("5 - Listar fornecedores")
            println("6 - Listar funcionários")
            println("7 - Desativar pessoa")
            println("0 - Voltar")
            val opcao = Console.lerInteiro("Opção", 0, 7)
            if (opcao == 0) return

            executarComSeguranca {
                when (opcao) {
                    1 -> cadastrarCliente()
                    2 -> cadastrarFornecedor()
                    3 -> cadastrarFuncionario()
                    4 -> listar(TipoPessoa.CLIENTE)
                    5 -> listar(TipoPessoa.FORNECEDOR)
                    6 -> listar(TipoPessoa.FUNCIONARIO)
                    7 -> desativar()
                }
            }
        }
    }

    private fun cadastrarCliente() {
        println("\n-- Novo cliente (digite 'cancelar' para desistir) --")
        val nome = Console.lerTexto("Nome")
        val cpf = Console.lerTexto("CPF (000.000.000-00)", Validador.REGEX_CPF, "CPF deve estar no formato 000.000.000-00")
        val email = Console.lerTextoOpcional("E-mail", Validador.REGEX_EMAIL, "E-mail inválido (ex.: nome@dominio.com)")
        val telefone = Console.lerTextoOpcional("Telefone", Validador.REGEX_TELEFONE, "Telefone deve estar no formato (11) 91234-5678")
        salvar(Cliente(null, nome, cpf, email, telefone))
    }

    private fun cadastrarFornecedor() {
        println("\n-- Novo fornecedor (digite 'cancelar' para desistir) --")
        val nome = Console.lerTexto("Nome / razão social")
        val cnpj = Console.lerTexto("CNPJ (00.000.000/0000-00)", Validador.REGEX_CNPJ, "CNPJ deve estar no formato 00.000.000/0000-00")
        val email = Console.lerTextoOpcional("E-mail", Validador.REGEX_EMAIL, "E-mail inválido (ex.: nome@dominio.com)")
        val telefone = Console.lerTextoOpcional("Telefone", Validador.REGEX_TELEFONE, "Telefone deve estar no formato (11) 3333-4444")
        salvar(Fornecedor(null, nome, cnpj, email, telefone))
    }

    private fun cadastrarFuncionario() {
        println("\n-- Novo funcionário (digite 'cancelar' para desistir) --")
        val nome = Console.lerTexto("Nome")
        val cpf = Console.lerTexto("CPF (000.000.000-00)", Validador.REGEX_CPF, "CPF deve estar no formato 000.000.000-00")
        val email = Console.lerTextoOpcional("E-mail", Validador.REGEX_EMAIL, "E-mail inválido (ex.: nome@dominio.com)")
        val telefone = Console.lerTextoOpcional("Telefone", Validador.REGEX_TELEFONE, "Telefone deve estar no formato (11) 91234-5678")

        // Setor: mostra a lista e só aceita um id que exista
        val setores = Database.conectar().use { conn -> setorRepo.listar(conn) }
        println("Setores disponíveis:")
        for (setor in setores) {
            println("  ${setor.id} - ${setor.nome}")
        }
        var setorId: Int
        while (true) {
            setorId = Console.lerInteiro("Id do setor", 1)
            if (setores.any { it.id == setorId }) break
            println("  ! Setor $setorId não existe.")
        }

        // Cargo: mostra os valores do enum numerados
        println("Cargos:")
        val cargos = Cargo.values()
        for (i in cargos.indices) {
            println("  ${i + 1} - ${cargos[i]}")
        }
        val cargo = cargos[Console.lerInteiro("Cargo", 1, cargos.size) - 1]

        // lerDecimal com mínimo 0,01: salário negativo ou zero nem chega a criar o objeto
        val salario = Console.lerDecimal("Salário (ex.: 2500,00)", BigDecimal("0.01"))

        salvar(Funcionario(null, nome, cpf, email, telefone, true, setorId, cargo, salario))
    }

    // Recebe qualquer Pessoa (Cliente, Fornecedor ou Funcionario) e usa o validar() de cada uma
    private fun salvar(pessoa: Pessoa) {
        val erros = pessoa.validar()
        if (erros.isNotEmpty()) {
            println("\nCadastro NÃO realizado. Corrija os erros:")
            for (erro in erros) {
                println("  - $erro")
            }
            return
        }
        val id = Database.emTransacao { conn ->
            if (pessoaRepo.buscarPorDocumento(conn, pessoa.documento) != null) {
                throw NegocioException("Já existe cadastro com o documento ${pessoa.documento}.")
            }
            pessoaRepo.inserir(conn, pessoa)
        }
        println("\nCadastrado com sucesso! Id gerado: $id")
    }

    private fun listar(tipo: TipoPessoa) {
        println("\n-- ${tipo.name} --")
        val lista = Database.conectar().use { conn -> pessoaRepo.listarPorTipo(conn, tipo) }
        if (lista.isEmpty()) {
            println("Nenhum registro.")
        }
        for (pessoa in lista) {
            println(pessoa)   // chama descricao(): Funcionario mostra cargo/salário, os outros não
            println("     e-mail: ${pessoa.email ?: "-"} | telefone: ${pessoa.telefone ?: "-"}")
        }
        Console.pausar()
    }

    private fun desativar() {
        val id = Console.lerInteiro("Id da pessoa a desativar", 1)
        if (id == Empresa.ID) throw NegocioException("A empresa não pode ser desativada.")
        val pessoa = Database.conectar().use { conn -> pessoaRepo.buscarPorId(conn, id) }
            ?: throw NegocioException("Pessoa $id não encontrada.")
        println("Encontrado: $pessoa")
        if (Console.confirmar("Confirma a desativação?")) {
            Database.emTransacao { conn -> pessoaRepo.desativar(conn, id) }
            println("Pessoa desativada.")
        }
    }
}
