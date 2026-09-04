package br.com.bikeshop.service

import br.com.bikeshop.model.Funcionario

// Guarda o funcionário logado (null antes do login). O Caixa usa como responsável da movimentação.
object Sessao {
    var funcionarioLogado: Funcionario? = null
}
