package br.com.bikeshop.model

// Interface do que precisa ser validado antes de salvar. Devolve a lista de erros (vazia = válido).
interface Validavel {
    fun validar(): List<String>
}
