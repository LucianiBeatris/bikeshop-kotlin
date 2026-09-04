package br.com.bikeshop.util

// Validações com regex. CPF e CNPJ também conferem os dígitos verificadores.
object Validador {

    // CPF 000.000.000-00  (^ início, \d{3} três dígitos, \. ponto, - hífen, \d{2} dois dígitos, $ fim)
    val REGEX_CPF = Regex("""^\d{3}\.\d{3}\.\d{3}-\d{2}$""")

    // CNPJ 00.000.000/0000-00
    val REGEX_CNPJ = Regex("""^\d{2}\.\d{3}\.\d{3}/\d{4}-\d{2}$""")

    // E-mail algo@dominio.com  (parte antes do @, arroba, domínio, um ou mais ".algo")
    val REGEX_EMAIL = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+$""")

    // Telefone (11) 91234-5678 ou (11) 3333-4444  (9? = nono dígito opcional)
    val REGEX_TELEFONE = Regex("""^\(\d{2}\) 9?\d{4}-\d{4}$""")

    fun emailValido(email: String): Boolean = REGEX_EMAIL.matches(email)

    fun telefoneValido(telefone: String): Boolean = REGEX_TELEFONE.matches(telefone)

    // formato certo (regex) + dígitos verificadores
    fun cpfValido(cpf: String): Boolean {
        if (!REGEX_CPF.matches(cpf)) return false
        val numeros = cpf.filter { it.isDigit() }.map { it.digitToInt() }
        if (numeros.all { it == numeros[0] }) return false // 111.111.111-11 passa na regex mas é inválido
        val dv1 = calcularDigito(numeros.take(9), 10)
        val dv2 = calcularDigito(numeros.take(10), 11)
        return numeros[9] == dv1 && numeros[10] == dv2
    }

    // formato certo (regex) + dígitos verificadores
    fun cnpjValido(cnpj: String): Boolean {
        if (!REGEX_CNPJ.matches(cnpj)) return false
        val numeros = cnpj.filter { it.isDigit() }.map { it.digitToInt() }
        if (numeros.all { it == numeros[0] }) return false
        val pesos1 = listOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        val pesos2 = listOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        val dv1 = calcularDigitoCnpj(numeros.take(12), pesos1)
        val dv2 = calcularDigitoCnpj(numeros.take(13), pesos2)
        return numeros[12] == dv1 && numeros[13] == dv2
    }

    // cálculo do dígito verificador do CPF (peso decrescente, resto da divisão por 11)
    private fun calcularDigito(digitos: List<Int>, pesoInicial: Int): Int {
        var soma = 0
        var peso = pesoInicial
        for (d in digitos) {
            soma += d * peso
            peso--
        }
        val resto = soma % 11
        return if (resto < 2) 0 else 11 - resto
    }

    private fun calcularDigitoCnpj(digitos: List<Int>, pesos: List<Int>): Int {
        var soma = 0
        for (i in digitos.indices) {
            soma += digitos[i] * pesos[i]
        }
        val resto = soma % 11
        return if (resto < 2) 0 else 11 - resto
    }
}
