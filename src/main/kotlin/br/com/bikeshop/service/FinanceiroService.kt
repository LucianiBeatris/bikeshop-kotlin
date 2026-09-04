package br.com.bikeshop.service

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.Empresa
import br.com.bikeshop.model.Fornecedor
import br.com.bikeshop.model.Funcionario
import br.com.bikeshop.model.MovimentacaoFinanceira
import br.com.bikeshop.model.OrigemMovimentacao
import br.com.bikeshop.model.TipoMovimentacao
import br.com.bikeshop.model.TipoPessoa
import br.com.bikeshop.repository.PessoaRepository
import java.math.BigDecimal

// Salários, aporte de capital e despesas avulsas
class FinanceiroService {

    private val caixa = Caixa()
    private val pessoaRepo = PessoaRepository()

    fun consultarSaldo(): BigDecimal {
        Database.conectar().use { conn ->
            return caixa.saldo(conn)
        }
    }

    fun extrato(tipo: TipoMovimentacao? = null): List<MovimentacaoFinanceira> {
        Database.conectar().use { conn ->
            return caixa.extrato(conn, tipo)
        }
    }

    fun extratoPorResponsavel(responsavelId: Int): List<MovimentacaoFinanceira> {
        Database.conectar().use { conn ->
            return caixa.extratoPorResponsavel(conn, responsavelId)
        }
    }

    // Paga o salário de um funcionário: sai do caixa da empresa e vai para o funcionário
    fun pagarSalario(funcionarioId: Int, referencia: String): MovimentacaoFinanceira {
        return Database.emTransacao { conn ->
            val funcionario = pessoaRepo.buscarPorId(conn, funcionarioId) as? Funcionario
                ?: throw NegocioException("Funcionário $funcionarioId não encontrado.")
            if (!funcionario.ativo) throw NegocioException("Funcionário está inativo.")
            caixa.registrarSaida(
                conn, funcionario.salario, funcionarioId,
                OrigemMovimentacao.SALARIO, "Salário $referencia - ${funcionario.nome}"
            )
        }
    }

    // Folha completa numa única transação: se faltar dinheiro para um, ninguém recebe (rollback)
    fun pagarTodosSalarios(referencia: String): List<MovimentacaoFinanceira> {
        return Database.emTransacao { conn ->
            val pagamentos = mutableListOf<MovimentacaoFinanceira>()
            for (pessoa in pessoaRepo.listarPorTipo(conn, TipoPessoa.FUNCIONARIO)) {
                if (pessoa is Funcionario && pessoa.ativo) {
                    val id = pessoa.id ?: continue
                    pagamentos.add(
                        caixa.registrarSaida(conn, pessoa.salario, id, OrigemMovimentacao.SALARIO, "Salário $referencia - ${pessoa.nome}")
                    )
                }
            }
            if (pagamentos.isEmpty()) throw NegocioException("Nenhum funcionário ativo para pagar.")
            pagamentos
        }
    }

    // Dono coloca dinheiro na empresa (necessário para começar a operar, já que o caixa inicia zerado)
    fun aporteCapital(valor: BigDecimal, descricao: String): MovimentacaoFinanceira {
        return Database.emTransacao { conn ->
            caixa.registrarEntrada(conn, valor, Empresa.ID, OrigemMovimentacao.APORTE, descricao)
        }
    }

    // Despesa avulsa paga a um fornecedor (ex.: conta de luz, aluguel)
    fun pagarDespesa(valor: BigDecimal, fornecedorId: Int, descricao: String): MovimentacaoFinanceira {
        return Database.emTransacao { conn ->
            val fornecedor = pessoaRepo.buscarPorId(conn, fornecedorId) as? Fornecedor
                ?: throw NegocioException("Fornecedor $fornecedorId não encontrado.")
            caixa.registrarSaida(conn, valor, fornecedorId, OrigemMovimentacao.DESPESA, "$descricao - ${fornecedor.nome}")
        }
    }
}
