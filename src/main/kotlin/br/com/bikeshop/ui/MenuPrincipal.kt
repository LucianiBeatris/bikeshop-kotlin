package br.com.bikeshop.ui

import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.excecoes.OperacaoCanceladaException
import br.com.bikeshop.service.Sessao
import br.com.bikeshop.util.Console
import java.sql.SQLException

// Roda a ação do menu com try/catch: erro vira mensagem na tela e volta pro menu
fun executarComSeguranca(acao: () -> Unit) {
    try {
        acao()
    } catch (e: OperacaoCanceladaException) {
        println("\n>> Operação cancelada. Nada foi salvo.")
    } catch (e: NegocioException) {
        println("\n>> Não foi possível concluir: ${e.message}")
    } catch (e: SQLException) {
        println("\n>> Erro no banco de dados: ${e.message}")
    } catch (e: Exception) {
        println("\n>> Erro inesperado: ${e.message}")
    }
}

class MenuPrincipal {

    fun executar() {
        while (true) {
            println("\n========== BIKESHOP - MENU PRINCIPAL ==========")
            println("Usuário logado: ${Sessao.funcionarioLogado?.nome ?: "-"}")
            println("1 - Pessoas (clientes, fornecedores, funcionários)")
            println("2 - Setores")
            println("3 - Produtos e estoque")
            println("4 - Vendas")
            println("5 - Compras de fornecedor")
            println("6 - Ordens de serviço (manutenção)")
            println("7 - Financeiro / Caixa")
            println("8 - Trocar usuário")
            println("0 - Sair")

            val opcao = try {
                Console.lerInteiro("Opção", 0, 8)
            } catch (e: OperacaoCanceladaException) {
                0
            }

            when (opcao) {
                1 -> MenuPessoas().executar()
                2 -> MenuSetores().executar()
                3 -> MenuProdutos().executar()
                4 -> MenuVendas().executar()
                5 -> MenuCompras().executar()
                6 -> MenuServicos().executar()
                7 -> MenuFinanceiro().executar()
                8 -> if (!TelaLogin.autenticar()) return
                0 -> return
            }
        }
    }
}
