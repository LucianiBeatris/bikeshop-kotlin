package br.com.bikeshop.model

// Enums do sistema. No banco vão como texto (.name) e voltam com valueOf().

// tipo da linha na tabela pessoa
enum class TipoPessoa { EMPRESA, FUNCIONARIO, CLIENTE, FORNECEDOR }

// cargos do funcionário (para criar um novo, ex. ESTAGIARIO, é só adicionar aqui)
enum class Cargo(val descricao: String) {
    GERENTE("Gerente"),
    VENDEDOR("Vendedor"),
    MECANICO("Mecânico"),
    CAIXA("Operador de caixa"),
    ESTOQUISTA("Estoquista");

    override fun toString(): String = descricao
}

// entrada soma no caixa (+1), saída subtrai (-1)
enum class TipoMovimentacao(val sinal: Int) {
    ENTRADA(+1),
    SAIDA(-1)
}

// qual operação gerou a movimentação
enum class OrigemMovimentacao { VENDA, COMPRA, SERVICO, SALARIO, APORTE, DESPESA }

// status da ordem de serviço
enum class StatusOrdemServico { ABERTA, EM_ANDAMENTO, CONCLUIDA, CANCELADA }
