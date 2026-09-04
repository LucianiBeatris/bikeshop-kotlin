package br.com.bikeshop.util

import br.com.bikeshop.excecoes.OperacaoCanceladaException
import java.math.BigDecimal
import java.math.RoundingMode

// Leitura do teclado. Repete a pergunta até vir um valor válido. "cancelar" desiste da operação.
object Console {

    const val PALAVRA_CANCELAR = "cancelar"

    // Lê uma linha. Se a entrada foi fechada (Ctrl+Z) ou o usuário digitou "cancelar", cancela a operação.
    private fun lerLinha(rotulo: String): String {
        print("$rotulo: ")
        val linha = readLine() ?: throw OperacaoCanceladaException()
        val texto = linha.trim()
        if (texto.equals(PALAVRA_CANCELAR, ignoreCase = true)) {
            throw OperacaoCanceladaException()
        }
        return texto
    }

    // Texto obrigatório. Enter vazio repete a pergunta. Se tiver regex, o texto precisa bater com ela.
    fun lerTexto(rotulo: String, regex: Regex? = null, mensagemErro: String = "Formato inválido."): String {
        while (true) {
            val texto = lerLinha(rotulo)
            if (texto.isEmpty()) {
                println("  ! Campo obrigatório. Digite um valor (ou '$PALAVRA_CANCELAR').")
            } else if (regex != null && !regex.matches(texto)) {
                println("  ! $mensagemErro")
            } else {
                return texto
            }
        }
    }

    // Texto opcional: Enter vazio devolve null (por isso o retorno é String?).
    fun lerTextoOpcional(rotulo: String, regex: Regex? = null, mensagemErro: String = "Formato inválido."): String? {
        while (true) {
            val texto = lerLinha("$rotulo (opcional, Enter para pular)")
            if (texto.isEmpty()) {
                return null
            } else if (regex != null && !regex.matches(texto)) {
                println("  ! $mensagemErro")
            } else {
                return texto
            }
        }
    }

    // Número inteiro entre minimo e maximo. toIntOrNull devolve null se não for número.
    fun lerInteiro(rotulo: String, minimo: Int = 0, maximo: Int = Int.MAX_VALUE): Int {
        while (true) {
            val texto = lerLinha(rotulo)
            val numero = texto.toIntOrNull()
            if (numero == null) {
                println("  ! Digite um número inteiro válido.")
            } else if (numero < minimo || numero > maximo) {
                println("  ! Digite um número entre $minimo e $maximo.")
            } else {
                return numero
            }
        }
    }

    // Valor em dinheiro. Aceita vírgula ou ponto. Não aceita texto nem valor abaixo do mínimo.
    fun lerDecimal(rotulo: String, minimo: BigDecimal = BigDecimal.ZERO): BigDecimal {
        while (true) {
            val texto = lerLinha(rotulo).replace(",", ".")
            val numero = texto.toBigDecimalOrNull()
            if (numero == null) {
                println("  ! Digite um valor numérico válido (ex.: 150,00).")
            } else if (numero < minimo) {
                println("  ! O valor mínimo permitido é $minimo.")
            } else {
                return numero.setScale(2, RoundingMode.HALF_UP)
            }
        }
    }

    // Pergunta sim/não.
    fun confirmar(pergunta: String): Boolean {
        while (true) {
            val resposta = lerLinha("$pergunta (s/n)").lowercase()
            if (resposta == "s" || resposta == "sim") return true
            if (resposta == "n" || resposta == "nao" || resposta == "não") return false
            println("  ! Responda com 's' ou 'n'.")
        }
    }

    fun pausar() {
        print("\n[Enter para continuar]")
        readLine()
    }
}
