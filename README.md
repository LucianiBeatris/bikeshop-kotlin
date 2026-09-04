# BikeShop - Sistema de gestão de loja e oficina de bicicletas

Trabalho individual da disciplina. Aplicação de console em **Kotlin** com persistência em **PostgreSQL**.

## O que o sistema faz

- **Menus interativos** no console.
- **Pessoas**: cadastro de clientes (CPF), fornecedores (CNPJ) e funcionários (CPF + setor + cargo + salário).
- **Setores**: Administrativo, Financeiro, Vendas e Oficina já vêm criados; é possível criar outros e transferir funcionários.
- **Produtos e estoque**: cadastro, listagem e alerta de estoque baixo.
- **Compra de fornecedor**: paga o fornecedor (sai dinheiro do caixa) e aumenta o estoque.
- **Venda para cliente**: baixa o estoque e recebe o dinheiro (entra no caixa).
- **Ordem de serviço (manutenção)**: abrir, iniciar, concluir (cliente paga) e cancelar.
- **Financeiro**: saldo do caixa, extrato, pagamento de salários, aporte de capital, despesas e auditoria por responsável.
- **Toda operação com dinheiro** gera uma linha em `movimentacao_financeira` com: valor, pagador, recebedor, data/hora, descrição e funcionário responsável.
- **Validações**: regex (CPF, CNPJ, e-mail, telefone), dígitos verificadores de CPF/CNPJ, `try/catch`, tipos nullable, transações com rollback e `CHECK` no banco.

### - Login inicial

O sistema pede o CPF de um funcionário. O administrador padrão é:

```
CPF: 123.456.789-09
```

### - Sugestão de roteiro para testar

1. Financeiro > Aporte de capital (o caixa começa zerado).
2. Compras > Registrar compra (do fornecedor de exemplo) para encher o estoque.
3. Vendas > Registrar venda para o cliente de exemplo.
4. Ordens de serviço > Abrir e depois Concluir.
5. Financeiro > Extrato completo para ver todas as movimentações.

Em qualquer pergunta é possível digitar `cancelar` para desistir da operação sem salvar nada.

## Estrutura do projeto

```
src/main/kotlin/br/com/bikeshop/
  Main.kt                 -> ponto de entrada
  db/Database.kt          -> conexão com o PostgreSQL e transações
  model/                  -> classes de domínio (Pessoa, Funcionario, Produto, Venda, MovimentacaoFinanceira...)
  repository/             -> SQL (INSERT/SELECT/UPDATE) de cada tabela
  service/                -> regras de negócio (Caixa, VendaService, CompraService, OrdemServicoService...)
  ui/                     -> menus de console
  util/                   -> leitura do teclado (Console), regex (Validador), formatação
  excecoes/               -> exceções próprias do sistema
src/main/resources/schema.sql -> script de criação das tabelas e dados iniciais
```
