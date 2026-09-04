package br.com.bikeshop.service

import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.excecoes.SaldoInsuficienteException
import br.com.bikeshop.model.Empresa
import br.com.bikeshop.model.MovimentacaoFinanceira
import br.com.bikeshop.model.OrigemMovimentacao
import br.com.bikeshop.model.TipoMovimentacao
import br.com.bikeshop.repository.MovimentacaoRepository
import java.math.BigDecimal
import java.sql.Connection
import java.time.LocalDateTime

// Caixa da empresa. Único lugar que mexe no saldo e grava movimentações.
// Não tem setSaldo: o saldo só muda por registrarEntrada / registrarSaida.
class Caixa {

    private val repositorio = MovimentacaoRepository()

    fun saldo(conn: Connection): BigDecimal {
        return repositorio.lerSaldo(conn)
    }

    // Dinheiro ENTRANDO na empresa (venda, serviço, aporte). Quem recebe é sempre a empresa.
    fun registrarEntrada(
        conn: Connection,
        valor: BigDecimal,
        pagadorId: Int,
        origem: OrigemMovimentacao,
        descricao: String
    ): MovimentacaoFinanceira {
        return registrar(conn, TipoMovimentacao.ENTRADA, valor, pagadorId, Empresa.ID, origem, descricao)
    }

    // Dinheiro SAINDO da empresa (compra, salário, despesa). Quem paga é sempre a empresa.
    fun registrarSaida(
        conn: Connection,
        valor: BigDecimal,
        recebedorId: Int,
        origem: OrigemMovimentacao,
        descricao: String
    ): MovimentacaoFinanceira {
        return registrar(conn, TipoMovimentacao.SAIDA, valor, Empresa.ID, recebedorId, origem, descricao)
    }

    // aqui toda movimentação é criada (responsável vem da Sessao, id vem do banco)
    private fun registrar(
        conn: Connection,
        tipo: TipoMovimentacao,
        valor: BigDecimal,
        pagadorId: Int,
        recebedorId: Int,
        origem: OrigemMovimentacao,
        descricao: String
    ): MovimentacaoFinanceira {
        if (valor <= BigDecimal.ZERO) {
            throw NegocioException("O valor da movimentação deve ser maior que zero.")
        }
        if (descricao.isBlank()) {
            throw NegocioException("A movimentação precisa de uma descrição.")
        }
        // elvis (?:): se não tiver ninguém logado, não deixa registrar
        val responsavel = Sessao.funcionarioLogado ?: throw NegocioException("Nenhum funcionário logado.")
        val responsavelId = responsavel.id ?: throw NegocioException("Funcionário logado sem id.")

        // ENTRADA soma (+valor), SAIDA subtrai (-valor). Se ficaria negativo, o banco devolve null.
        val variacao = valor.multiply(BigDecimal(tipo.sinal))
        val novoSaldo = repositorio.atualizarSaldo(conn, variacao)
        if (novoSaldo == null) {
            throw SaldoInsuficienteException(valor, repositorio.lerSaldo(conn))
        }

        val movimentacao = MovimentacaoFinanceira(
            id = null,
            tipo = tipo,
            origem = origem,
            valor = valor,
            pagadorId = pagadorId,
            recebedorId = recebedorId,
            responsavelId = responsavelId,
            dataHora = LocalDateTime.now(),
            descricao = descricao
        )
        val id = repositorio.inserir(conn, movimentacao)
        return movimentacao.copy(id = id)
    }

    fun extrato(conn: Connection, tipo: TipoMovimentacao? = null): List<MovimentacaoFinanceira> {
        return repositorio.listar(conn, tipo)
    }

    fun extratoPorResponsavel(conn: Connection, responsavelId: Int): List<MovimentacaoFinanceira> {
        return repositorio.listarPorResponsavel(conn, responsavelId)
    }
}
