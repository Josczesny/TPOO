package com.vendas.database;

import java.sql.*;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;
    private static final String DB_PATH = System.getProperty("user.home") + "/sistema_vendas.db";

    private DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            createTables();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao conectar ao banco: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao reconectar: " + e.getMessage(), e);
        }
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS categoria (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome     VARCHAR(100) NOT NULL,
                    descricao VARCHAR(255)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS produto (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    codigo         VARCHAR(20)    NOT NULL UNIQUE,
                    nome           VARCHAR(100)   NOT NULL,
                    descricao      VARCHAR(255),
                    preco          DECIMAL(10,2)  NOT NULL,
                    estoque        INTEGER        NOT NULL DEFAULT 0,
                    estoque_minimo INTEGER        NOT NULL DEFAULT 5,
                    id_categoria   INTEGER,
                    FOREIGN KEY (id_categoria) REFERENCES categoria(id)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS cliente (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome           VARCHAR(100) NOT NULL,
                    cpf            VARCHAR(14)  NOT NULL UNIQUE,
                    email          VARCHAR(100),
                    telefone       VARCHAR(20),
                    endereco       VARCHAR(255),
                    data_cadastro  DATE NOT NULL
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS orcamento (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero         VARCHAR(20)   NOT NULL UNIQUE,
                    id_cliente     INTEGER       NOT NULL,
                    data_emissao   DATE          NOT NULL,
                    data_validade  DATE          NOT NULL,
                    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
                    valor_total    DECIMAL(10,2) NOT NULL DEFAULT 0,
                    observacao     VARCHAR(255),
                    FOREIGN KEY (id_cliente) REFERENCES cliente(id)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS item_orcamento (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_orcamento    INTEGER       NOT NULL,
                    id_produto      INTEGER       NOT NULL,
                    quantidade      INTEGER       NOT NULL,
                    preco_unitario  DECIMAL(10,2) NOT NULL,
                    subtotal        DECIMAL(10,2) NOT NULL,
                    FOREIGN KEY (id_orcamento) REFERENCES orcamento(id),
                    FOREIGN KEY (id_produto)   REFERENCES produto(id)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS venda (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero          VARCHAR(20)   NOT NULL UNIQUE,
                    id_cliente      INTEGER       NOT NULL,
                    id_orcamento    INTEGER,
                    data_venda      DATETIME      NOT NULL,
                    forma_pagamento VARCHAR(30)   NOT NULL,
                    valor_total     DECIMAL(10,2) NOT NULL DEFAULT 0,
                    desconto        DECIMAL(10,2) NOT NULL DEFAULT 0,
                    valor_final     DECIMAL(10,2) NOT NULL DEFAULT 0,
                    status          VARCHAR(20)   NOT NULL DEFAULT 'CONCLUIDA',
                    observacao      VARCHAR(255),
                    FOREIGN KEY (id_cliente)   REFERENCES cliente(id),
                    FOREIGN KEY (id_orcamento) REFERENCES orcamento(id)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS item_venda (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_venda       INTEGER       NOT NULL,
                    id_produto     INTEGER       NOT NULL,
                    quantidade     INTEGER       NOT NULL,
                    preco_unitario DECIMAL(10,2) NOT NULL,
                    subtotal       DECIMAL(10,2) NOT NULL,
                    FOREIGN KEY (id_venda)   REFERENCES venda(id),
                    FOREIGN KEY (id_produto) REFERENCES produto(id)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS devolucao (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero          VARCHAR(20)   NOT NULL UNIQUE,
                    id_venda        INTEGER       NOT NULL,
                    data_devolucao  DATETIME      NOT NULL,
                    motivo          VARCHAR(255)  NOT NULL,
                    valor_devolvido DECIMAL(10,2) NOT NULL DEFAULT 0,
                    FOREIGN KEY (id_venda) REFERENCES venda(id)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS item_devolucao (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_devolucao   INTEGER       NOT NULL,
                    id_produto     INTEGER       NOT NULL,
                    quantidade     INTEGER       NOT NULL,
                    preco_unitario DECIMAL(10,2) NOT NULL,
                    subtotal       DECIMAL(10,2) NOT NULL,
                    FOREIGN KEY (id_devolucao) REFERENCES devolucao(id),
                    FOREIGN KEY (id_produto)   REFERENCES produto(id)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS troca (
                    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero               VARCHAR(20)   NOT NULL UNIQUE,
                    id_venda             INTEGER       NOT NULL,
                    data_troca           DATETIME      NOT NULL,
                    motivo               VARCHAR(255)  NOT NULL,
                    id_produto_devolvido INTEGER       NOT NULL,
                    quantidade_devolvida INTEGER       NOT NULL,
                    id_produto_novo      INTEGER       NOT NULL,
                    quantidade_nova      INTEGER       NOT NULL,
                    valor_diferenca      DECIMAL(10,2) NOT NULL DEFAULT 0,
                    FOREIGN KEY (id_venda)             REFERENCES venda(id),
                    FOREIGN KEY (id_produto_devolvido) REFERENCES produto(id),
                    FOREIGN KEY (id_produto_novo)      REFERENCES produto(id)
                )
            """);
        }
    }
}
