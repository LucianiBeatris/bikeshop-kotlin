package br.com.bikeshop

import br.com.bikeshop.db.Database
import br.com.bikeshop.ui.MenuPrincipal
import br.com.bikeshop.ui.TelaLogin
import java.sql.SQLException

// Início do programa: conecta no banco, pede login e abre o menu
fun main() {
    println("=== BikeShop - Sistema de gestão de loja e oficina de bicicletas ===")

    try {
        Database.inicializar()
        println("Conectado ao banco de dados.")
    } catch (e: SQLException) {
        println("Não foi possível conectar ao PostgreSQL: ${e.message}")
        println("Verifique se o PostgreSQL está rodando e se usuário/senha estão corretos (veja o README.md).")
        return
    }

    if (!TelaLogin.autenticar()) {
        println("Nenhum usuário logado. Encerrando.")
        return
    }

    MenuPrincipal().executar()
    println("Até logo!")
}
