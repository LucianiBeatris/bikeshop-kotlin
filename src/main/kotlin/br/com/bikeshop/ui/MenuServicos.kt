package br.com.bikeshop.ui

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.TipoPessoa
import br.com.bikeshop.repository.PessoaRepository
import br.com.bikeshop.service.OrdemServicoService
import br.com.bikeshop.util.Console
import br.com.bikeshop.util.Formatador
import java.math.BigDecimal

// Ordens de serviço da oficina
class MenuServicos {

    private val osService = OrdemServicoService()
    private val pessoaRepo = PessoaRepository()

    fun executar() {
        while (true) {
            println("\n---------- ORDENS DE SERVIÇO ----------")
            println("1 - Abrir ordem de serviço")
            println("2 - Listar ordens de serviço")
            println("3 - Iniciar serviço")
            println("4 - Concluir serviço (cliente paga)")
            println("5 - Cancelar ordem de serviço")
            println("0 - Voltar")
            val opcao = Console.lerInteiro("Opção", 0, 5)
            if (opcao == 0) return

            executarComSeguranca {
                when (opcao) {
                    1 -> abrir()
                    2 -> listar()
                    3 -> {
                        osService.iniciar(Console.lerInteiro("Id da OS", 1))
                        println("OS em andamento.")
                    }
                    4 -> {
                        val os = osService.concluir(Console.lerInteiro("Id da OS", 1))
                        println("OS concluída e ${Formatador.moeda(os.valor)} recebidos (movimentação #${os.movimentacaoId}).")
                    }
                    5 -> {
                        osService.cancelar(Console.lerInteiro("Id da OS", 1))
                        println("OS cancelada.")
                    }
                }
            }
        }
    }

    private fun abrir() {
        println("\n-- Nova ordem de serviço (digite 'cancelar' para desistir) --")
        Database.conectar().use { conn ->
            println("Clientes:")
            pessoaRepo.listarPorTipo(conn, TipoPessoa.CLIENTE).forEach { println("  $it") }
        }
        val clienteId = Console.lerInteiro("Id do cliente", 1)

        Database.conectar().use { conn ->
            println("Funcionários (mecânicos):")
            pessoaRepo.listarPorTipo(conn, TipoPessoa.FUNCIONARIO).forEach { println("  $it") }
        }
        val mecanicoId = Console.lerInteiro("Id do funcionário responsável pelo serviço", 1)

        val descricao = Console.lerTexto("Descrição do serviço")
        val valor = Console.lerDecimal("Valor do serviço", BigDecimal("0.01"))

        val os = osService.abrir(clienteId, mecanicoId, descricao, valor)
        println("\nOS #${os.id} aberta para ${os.clienteNome}.")
    }

    private fun listar() {
        println()
        val lista = osService.listar()
        if (lista.isEmpty()) println("Nenhuma ordem de serviço.")
        lista.forEach { println(it) }
        Console.pausar()
    }
}
