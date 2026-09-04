package br.com.bikeshop.ui

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.TipoMovimentacao
import br.com.bikeshop.model.TipoPessoa
import br.com.bikeshop.repository.PessoaRepository
import br.com.bikeshop.service.FinanceiroService
import br.com.bikeshop.util.Console
import br.com.bikeshop.util.Formatador
import java.math.BigDecimal

// Caixa, extrato, salários e lançamentos avulsos
class MenuFinanceiro {

    private val financeiro = FinanceiroService()
    private val pessoaRepo = PessoaRepository()

    fun executar() {
        while (true) {
            println("\n---------- FINANCEIRO / CAIXA ----------")
            println("1 - Saldo do caixa")
            println("2 - Extrato completo")
            println("3 - Extrato de entradas")
            println("4 - Extrato de saídas")
            println("5 - Pagar salário de um funcionário")
            println("6 - Pagar salário de todos os funcionários")
            println("7 - Aporte de capital (colocar dinheiro no caixa)")
            println("8 - Pagar despesa avulsa a um fornecedor")
            println("9 - Auditoria: movimentações por responsável")
            println("0 - Voltar")
            val opcao = Console.lerInteiro("Opção", 0, 9)
            if (opcao == 0) return

            executarComSeguranca {
                when (opcao) {
                    1 -> println("\nSaldo atual do caixa: ${Formatador.moeda(financeiro.consultarSaldo())}")
                    2 -> mostrarExtrato(null)
                    3 -> mostrarExtrato(TipoMovimentacao.ENTRADA)
                    4 -> mostrarExtrato(TipoMovimentacao.SAIDA)
                    5 -> pagarSalario()
                    6 -> pagarTodos()
                    7 -> aporte()
                    8 -> despesa()
                    9 -> porResponsavel()
                }
            }
        }
    }

    private fun mostrarExtrato(tipo: TipoMovimentacao?) {
        println("\n-- Extrato ${tipo?.name ?: "COMPLETO"} --")
        val lista = financeiro.extrato(tipo)
        if (lista.isEmpty()) println("Nenhuma movimentação.")
        lista.forEach { println(it) }
        println("Saldo atual: ${Formatador.moeda(financeiro.consultarSaldo())}")
        Console.pausar()
    }

    private fun pagarSalario() {
        Database.conectar().use { conn ->
            println("Funcionários:")
            pessoaRepo.listarPorTipo(conn, TipoPessoa.FUNCIONARIO).forEach { println("  $it") }
        }
        val id = Console.lerInteiro("Id do funcionário", 1)
        val referencia = Console.lerTexto("Mês de referência (ex.: 09/2026)")
        if (!Console.confirmar("Confirmar pagamento?")) throw NegocioException("Pagamento não confirmado.")
        val mov = financeiro.pagarSalario(id, referencia)
        println("Pago: $mov")
    }

    private fun pagarTodos() {
        val referencia = Console.lerTexto("Mês de referência (ex.: 09/2026)")
        if (!Console.confirmar("Pagar TODOS os funcionários ativos?")) throw NegocioException("Pagamento não confirmado.")
        val pagamentos = financeiro.pagarTodosSalarios(referencia)
        pagamentos.forEach { println("Pago: $it") }
        println("Total da folha: ${Formatador.moeda(pagamentos.sumOf { it.valor })}")
    }

    private fun aporte() {
        val valor = Console.lerDecimal("Valor do aporte", BigDecimal("0.01"))
        val descricao = Console.lerTexto("Descrição (ex.: Capital inicial)")
        val mov = financeiro.aporteCapital(valor, descricao)
        println("Registrado: $mov")
    }

    private fun despesa() {
        Database.conectar().use { conn ->
            println("Fornecedores:")
            pessoaRepo.listarPorTipo(conn, TipoPessoa.FORNECEDOR).forEach { println("  $it") }
        }
        val fornecedorId = Console.lerInteiro("Id do fornecedor", 1)
        val valor = Console.lerDecimal("Valor", BigDecimal("0.01"))
        val descricao = Console.lerTexto("Descrição (ex.: Conta de luz)")
        val mov = financeiro.pagarDespesa(valor, fornecedorId, descricao)
        println("Registrado: $mov")
    }

    // Função de auditoria: mostra tudo que um funcionário registrou no caixa
    private fun porResponsavel() {
        Database.conectar().use { conn ->
            println("Funcionários:")
            pessoaRepo.listarPorTipo(conn, TipoPessoa.FUNCIONARIO).forEach { println("  $it") }
        }
        val id = Console.lerInteiro("Id do funcionário responsável", 1)
        val lista = financeiro.extratoPorResponsavel(id)
        println("\n${lista.size} movimentação(ões) registradas por esse funcionário:")
        lista.forEach { println(it) }
        Console.pausar()
    }
}
