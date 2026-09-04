package br.com.bikeshop.model

import br.com.bikeshop.util.Formatador
import br.com.bikeshop.util.Validador
import java.math.BigDecimal

// Classe base das pessoas. Cliente, Fornecedor, Funcionario e Empresa herdam dela.
abstract class Pessoa(
    val id: Int?,           // null enquanto a pessoa ainda não foi salva (o banco gera o id)
    val nome: String,
    val documento: String,  // CPF ou CNPJ já formatado
    val email: String?,     // opcional, por isso pode ser null
    val telefone: String?,  // opcional
    val ativo: Boolean = true
) : Validavel {

    // Cada subclasse diz qual é o seu tipo (vai para a coluna pessoa.tipo)
    abstract val tipo: TipoPessoa

    // Cada subclasse valida o documento do seu jeito (CPF ou CNPJ). Devolve a mensagem de erro ou null.
    protected abstract fun validarDocumento(): String?

    // Validação comum a todas as pessoas + validação do documento da subclasse
    override fun validar(): List<String> {
        val erros = mutableListOf<String>()
        if (nome.trim().length < 3) {
            erros.add("Nome deve ter pelo menos 3 caracteres.")
        }
        val erroDocumento = validarDocumento()
        if (erroDocumento != null) {
            erros.add(erroDocumento)
        }
        // "?.let" só roda se o campo não for null: e-mail em branco não gera erro
        email?.let { if (!Validador.emailValido(it)) erros.add("E-mail inválido: $it") }
        telefone?.let { if (!Validador.telefoneValido(it)) erros.add("Telefone inválido: $it") }
        return erros
    }

    // Texto mostrado nos menus. Subclasses podem mudar (ver Funcionario).
    open fun descricao(): String {
        val situacao = if (ativo) "" else " [INATIVO]"
        return "#$id - $nome - $documento$situacao"
    }

    override fun toString(): String = descricao()
}

// Cliente: pessoa física, identificada por CPF
class Cliente(
    id: Int?,
    nome: String,
    documento: String,
    email: String?,
    telefone: String?,
    ativo: Boolean = true
) : Pessoa(id, nome, documento, email, telefone, ativo) {

    override val tipo = TipoPessoa.CLIENTE

    override fun validarDocumento(): String? {
        return if (Validador.cpfValido(documento)) null else "CPF inválido: $documento"
    }
}

// Fornecedor: pessoa jurídica, identificada por CNPJ
class Fornecedor(
    id: Int?,
    nome: String,
    documento: String,
    email: String?,
    telefone: String?,
    ativo: Boolean = true
) : Pessoa(id, nome, documento, email, telefone, ativo) {

    override val tipo = TipoPessoa.FORNECEDOR

    override fun validarDocumento(): String? {
        return if (Validador.cnpjValido(documento)) null else "CNPJ inválido: $documento"
    }
}

// Funcionário: além dos dados de pessoa, tem setor, cargo e salário
class Funcionario(
    id: Int?,
    nome: String,
    documento: String,
    email: String?,
    telefone: String?,
    ativo: Boolean = true,
    val setorId: Int,
    val cargo: Cargo,
    val salario: BigDecimal
) : Pessoa(id, nome, documento, email, telefone, ativo) {

    override val tipo = TipoPessoa.FUNCIONARIO

    override fun validarDocumento(): String? {
        return if (Validador.cpfValido(documento)) null else "CPF inválido: $documento"
    }

    // Reaproveita a validação da classe mãe e acrescenta a regra do salário
    override fun validar(): List<String> {
        val erros = super.validar().toMutableList()
        if (salario <= BigDecimal.ZERO) {
            erros.add("Salário deve ser maior que zero.")
        }
        return erros
    }

    // Polimorfismo: o mesmo método descricao() mostra mais coisas quando o objeto é um Funcionario
    override fun descricao(): String {
        return super.descricao() + " - $cargo - ${Formatador.moeda(salario)} - setor $setorId"
    }
}

// A própria loja. Existe uma única linha dela no banco, com id fixo = 1 (ver schema.sql)
class Empresa(
    id: Int?,
    nome: String,
    documento: String,
    email: String?,
    telefone: String?,
    ativo: Boolean = true
) : Pessoa(id, nome, documento, email, telefone, ativo) {

    companion object {
        const val ID = 1
    }

    override val tipo = TipoPessoa.EMPRESA

    override fun validarDocumento(): String? {
        return if (Validador.cnpjValido(documento)) null else "CNPJ inválido: $documento"
    }
}
