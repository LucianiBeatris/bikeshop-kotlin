-- BikeShop - criação das tabelas (PostgreSQL)
-- Roda ao iniciar o programa (Database.inicializar()). Pode rodar várias vezes (IF NOT EXISTS / ON CONFLICT).

-- SETOR
CREATE TABLE IF NOT EXISTS setor (
    id        SERIAL PRIMARY KEY,
    nome      VARCHAR(60)  NOT NULL UNIQUE,
    descricao VARCHAR(200)                      -- opcional (pode ser NULL)
);

-- PESSOA: empresa, funcionários, clientes e fornecedores na mesma tabela (coluna tipo)
CREATE TABLE IF NOT EXISTS pessoa (
    id        SERIAL PRIMARY KEY,
    tipo      VARCHAR(20)  NOT NULL CHECK (tipo IN ('EMPRESA', 'FUNCIONARIO', 'CLIENTE', 'FORNECEDOR')),
    nome      VARCHAR(120) NOT NULL,
    documento VARCHAR(18)  NOT NULL UNIQUE,     -- CPF (000.000.000-00) ou CNPJ (00.000.000/0000-00)
    email     VARCHAR(120),                     -- opcional
    telefone  VARCHAR(20),                      -- opcional
    ativo     BOOLEAN      NOT NULL DEFAULT TRUE
);

-- FUNCIONARIO: dados só de funcionário. pessoa_id é PK e FK ao mesmo tempo.
CREATE TABLE IF NOT EXISTS funcionario (
    pessoa_id INTEGER       PRIMARY KEY REFERENCES pessoa(id),
    setor_id  INTEGER       NOT NULL REFERENCES setor(id),
    cargo     VARCHAR(30)   NOT NULL,
    salario   NUMERIC(12,2) NOT NULL CHECK (salario > 0)
);

-- PRODUTO / ESTOQUE
CREATE TABLE IF NOT EXISTS produto (
    id                 SERIAL PRIMARY KEY,
    nome               VARCHAR(120)  NOT NULL,
    descricao          VARCHAR(255),           -- opcional
    preco_custo        NUMERIC(12,2) NOT NULL CHECK (preco_custo >= 0),
    preco_venda        NUMERIC(12,2) NOT NULL CHECK (preco_venda > 0),
    quantidade_estoque INTEGER       NOT NULL DEFAULT 0 CHECK (quantidade_estoque >= 0),
    ativo              BOOLEAN       NOT NULL DEFAULT TRUE
);

-- CAIXA: uma linha só (id = 1) com o saldo atual. CHECK não deixa ficar negativo.
CREATE TABLE IF NOT EXISTS caixa (
    id    INTEGER       PRIMARY KEY CHECK (id = 1),
    saldo NUMERIC(14,2) NOT NULL CHECK (saldo >= 0)
);

-- MOVIMENTACAO_FINANCEIRA: extrato (valor, pagador, recebedor, data/hora, descrição, responsável)
CREATE TABLE IF NOT EXISTS movimentacao_financeira (
    id             SERIAL PRIMARY KEY,
    tipo           VARCHAR(10)   NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    origem         VARCHAR(20)   NOT NULL,     -- VENDA, COMPRA, SERVICO, SALARIO, APORTE, DESPESA
    valor          NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    pagador_id     INTEGER       NOT NULL REFERENCES pessoa(id),             -- quem pagou
    recebedor_id   INTEGER       NOT NULL REFERENCES pessoa(id),             -- quem recebeu
    responsavel_id INTEGER       NOT NULL REFERENCES funcionario(pessoa_id), -- funcionário logado que fez a operação
    data_hora      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descricao      VARCHAR(255)  NOT NULL
);

-- VENDA. movimentacao_id NOT NULL: não existe venda sem movimentação.
CREATE TABLE IF NOT EXISTS venda (
    id              SERIAL PRIMARY KEY,
    cliente_id      INTEGER       NOT NULL REFERENCES pessoa(id),
    vendedor_id     INTEGER       NOT NULL REFERENCES funcionario(pessoa_id),
    movimentacao_id INTEGER       NOT NULL UNIQUE REFERENCES movimentacao_financeira(id),
    data_hora       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total           NUMERIC(12,2) NOT NULL CHECK (total > 0)
);

-- ITEM_VENDA: ligação N para N entre venda e produto
CREATE TABLE IF NOT EXISTS item_venda (
    venda_id       INTEGER       NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
    produto_id     INTEGER       NOT NULL REFERENCES produto(id),
    quantidade     INTEGER       NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(12,2) NOT NULL CHECK (preco_unitario >= 0),
    PRIMARY KEY (venda_id, produto_id)
);

-- COMPRA e ITEM_COMPRA: mesma ideia da venda
CREATE TABLE IF NOT EXISTS compra (
    id              SERIAL PRIMARY KEY,
    fornecedor_id   INTEGER       NOT NULL REFERENCES pessoa(id),
    comprador_id    INTEGER       NOT NULL REFERENCES funcionario(pessoa_id),
    movimentacao_id INTEGER       NOT NULL UNIQUE REFERENCES movimentacao_financeira(id),
    data_hora       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total           NUMERIC(12,2) NOT NULL CHECK (total > 0)
);

CREATE TABLE IF NOT EXISTS item_compra (
    compra_id      INTEGER       NOT NULL REFERENCES compra(id) ON DELETE CASCADE,
    produto_id     INTEGER       NOT NULL REFERENCES produto(id),
    quantidade     INTEGER       NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(12,2) NOT NULL CHECK (preco_unitario >= 0),
    PRIMARY KEY (compra_id, produto_id)
);

-- ORDEM_SERVICO: manutenção. data_conclusao e movimentacao_id ficam NULL até concluir.
CREATE TABLE IF NOT EXISTS ordem_servico (
    id              SERIAL PRIMARY KEY,
    cliente_id      INTEGER       NOT NULL REFERENCES pessoa(id),
    mecanico_id     INTEGER       NOT NULL REFERENCES funcionario(pessoa_id),
    descricao       VARCHAR(255)  NOT NULL,
    valor           NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    status          VARCHAR(15)   NOT NULL DEFAULT 'ABERTA'
                    CHECK (status IN ('ABERTA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA')),
    data_abertura   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_conclusao  TIMESTAMP,                                                  -- NULL até concluir
    movimentacao_id INTEGER       UNIQUE REFERENCES movimentacao_financeira(id) -- NULL até receber
);

-- DADOS INICIAIS (só entram se ainda não existirem)
INSERT INTO setor (nome, descricao) VALUES
    ('Administrativo', 'Gerência e cadastros'),
    ('Financeiro',     'Caixa, pagamentos e recebimentos'),
    ('Vendas',         'Atendimento e vendas ao cliente'),
    ('Oficina',        'Manutenção e montagem de bicicletas')
ON CONFLICT (nome) DO NOTHING;

-- a própria empresa, id fixo 1
INSERT INTO pessoa (id, tipo, nome, documento, email, telefone)
VALUES (1, 'EMPRESA', 'BikeShop Ltda', '12.345.678/0001-95', 'contato@bikeshop.com', '(11) 4002-8922')
ON CONFLICT (id) DO NOTHING;

-- ajusta a sequência para não gerar o id 1 de novo
SELECT setval('pessoa_id_seq', GREATEST((SELECT MAX(id) FROM pessoa), 1));

INSERT INTO caixa (id, saldo) VALUES (1, 0) ON CONFLICT (id) DO NOTHING;

-- administrador padrão (login inicial), CPF 123.456.789-09
INSERT INTO pessoa (tipo, nome, documento, email, telefone)
VALUES ('FUNCIONARIO', 'Administrador', '123.456.789-09', 'admin@bikeshop.com', '(11) 99999-0000')
ON CONFLICT (documento) DO NOTHING;

INSERT INTO funcionario (pessoa_id, setor_id, cargo, salario)
SELECT p.id, s.id, 'GERENTE', 5000.00
  FROM pessoa p, setor s
 WHERE p.documento = '123.456.789-09' AND s.nome = 'Administrativo'
ON CONFLICT (pessoa_id) DO NOTHING;

-- cadastros de exemplo
INSERT INTO pessoa (tipo, nome, documento, email, telefone)
VALUES ('CLIENTE',    'Ana Souza',        '111.444.777-35',     'ana@email.com',         '(11) 98888-1111'),
       ('FORNECEDOR', 'Ciclo Peças Ltda', '11.222.333/0001-81', 'vendas@ciclopecas.com', '(11) 3333-2222')
ON CONFLICT (documento) DO NOTHING;

INSERT INTO produto (nome, descricao, preco_custo, preco_venda, quantidade_estoque)
SELECT 'Bicicleta Aro 29', 'Mountain bike 21 marchas', 900.00, 1500.00, 0
 WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Bicicleta Aro 29');

INSERT INTO produto (nome, descricao, preco_custo, preco_venda, quantidade_estoque)
SELECT 'Câmara de ar', NULL, 8.00, 20.00, 0
 WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Câmara de ar');
