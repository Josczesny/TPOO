package com.vendas.model;

import java.math.BigDecimal;

public class Produto {
    private int id;
    private String codigo;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private int estoque;
    private int estoqueMinimo;
    private int idCategoria;
    private String nomeCategoria;

    public Produto() {}

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public String getCodigo()                 { return codigo; }
    public void setCodigo(String c)           { this.codigo = c; }
    public String getNome()                   { return nome; }
    public void setNome(String n)             { this.nome = n; }
    public String getDescricao()              { return descricao; }
    public void setDescricao(String d)        { this.descricao = d; }
    public BigDecimal getPreco()              { return preco; }
    public void setPreco(BigDecimal p)        { this.preco = p; }
    public int getEstoque()                   { return estoque; }
    public void setEstoque(int e)             { this.estoque = e; }
    public int getEstoqueMinimo()             { return estoqueMinimo; }
    public void setEstoqueMinimo(int em)      { this.estoqueMinimo = em; }
    public int getIdCategoria()               { return idCategoria; }
    public void setIdCategoria(int ic)        { this.idCategoria = ic; }
    public String getNomeCategoria()          { return nomeCategoria; }
    public void setNomeCategoria(String nc)   { this.nomeCategoria = nc; }

    @Override public String toString() { return "[" + codigo + "] " + nome; }
}
