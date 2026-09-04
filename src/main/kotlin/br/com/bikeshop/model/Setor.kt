package br.com.bikeshop.model

// Setor da empresa (Administrativo, Financeiro, Vendas, Oficina...)
data class Setor(
    val id: Int?,
    val nome: String,
    val descricao: String?   // opcional
) : Validavel {

    override fun validar(): List<String> {
        val erros = mutableListOf<String>()
        if (nome.trim().length < 3) erros += "Nome do setor deve ter pelo menos 3 caracteres."
        return erros
    }
}
