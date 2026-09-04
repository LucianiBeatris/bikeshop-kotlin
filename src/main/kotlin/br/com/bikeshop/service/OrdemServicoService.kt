package br.com.bikeshop.service

import br.com.bikeshop.db.Database
import br.com.bikeshop.excecoes.NegocioException
import br.com.bikeshop.model.Cliente
import br.com.bikeshop.model.Funcionario
import br.com.bikeshop.model.OrdemServico
import br.com.bikeshop.model.OrigemMovimentacao
import br.com.bikeshop.model.StatusOrdemServico
import br.com.bikeshop.repository.OrdemServicoRepository
import br.com.bikeshop.repository.PessoaRepository
import java.math.BigDecimal

// Ordem de serviço. O cliente só paga quando a OS é concluída.
class OrdemServicoService {

    private val caixa = Caixa()
    private val pessoaRepo = PessoaRepository()
    private val osRepo = OrdemServicoRepository()

    fun abrir(clienteId: Int, mecanicoId: Int, descricao: String, valor: BigDecimal): OrdemServico {
        return Database.emTransacao { conn ->
            val cliente = pessoaRepo.buscarPorId(conn, clienteId) as? Cliente
                ?: throw NegocioException("Cliente $clienteId não encontrado.")
            val mecanico = pessoaRepo.buscarPorId(conn, mecanicoId) as? Funcionario
                ?: throw NegocioException("Funcionário $mecanicoId não encontrado.")

            val os = OrdemServico(null, clienteId, cliente.nome, mecanicoId, mecanico.nome, descricao, valor,
                StatusOrdemServico.ABERTA, null, null, null)
            val erros = os.validar()
            if (erros.isNotEmpty()) throw NegocioException(erros.joinToString(" "))

            val id = osRepo.inserir(conn, os)
            osRepo.buscarPorId(conn, id) ?: throw NegocioException("Erro ao gravar a OS.")
        }
    }

    fun iniciar(id: Int) {
        Database.emTransacao { conn ->
            val os = buscar(conn, id)
            if (os.status != StatusOrdemServico.ABERTA) throw NegocioException("Só é possível iniciar uma OS ABERTA (atual: ${os.status}).")
            osRepo.atualizarStatus(conn, id, StatusOrdemServico.EM_ANDAMENTO)
        }
    }

    // Concluir = o cliente paga: registra a ENTRADA no caixa e marca a OS como concluída
    fun concluir(id: Int): OrdemServico {
        return Database.emTransacao { conn ->
            val os = buscar(conn, id)
            if (os.status == StatusOrdemServico.CONCLUIDA || os.status == StatusOrdemServico.CANCELADA) {
                throw NegocioException("OS já está ${os.status}.")
            }
            val movimentacao = caixa.registrarEntrada(conn, os.valor, os.clienteId, OrigemMovimentacao.SERVICO, "Serviço OS #$id - ${os.descricao}")
            val movimentacaoId = movimentacao.id ?: throw NegocioException("Movimentação sem id.")
            osRepo.concluir(conn, id, movimentacaoId)
            buscar(conn, id)
        }
    }

    fun cancelar(id: Int) {
        Database.emTransacao { conn ->
            val os = buscar(conn, id)
            if (os.status == StatusOrdemServico.CONCLUIDA) throw NegocioException("OS concluída não pode ser cancelada.")
            osRepo.atualizarStatus(conn, id, StatusOrdemServico.CANCELADA)
        }
    }

    fun listar(): List<OrdemServico> {
        Database.conectar().use { conn ->
            return osRepo.listar(conn)
        }
    }

    private fun buscar(conn: java.sql.Connection, id: Int): OrdemServico {
        return osRepo.buscarPorId(conn, id) ?: throw NegocioException("OS $id não encontrada.")
    }
}
