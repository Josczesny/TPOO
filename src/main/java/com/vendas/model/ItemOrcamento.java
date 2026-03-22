package com.vendas.model;

import java.math.BigDecimal;

public class ItemOrcamento {
    private int id;
    private int idOrcamento;
    private int idProduto;
    private String nomeProduto;
    private int quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;

    public ItemOrcamento() {}

    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }
    public int getIdOrcamento()             { return idOrcamento; }
    public void setIdOrcamento(int i)       { this.idOrcamento = i; }
    public int getIdProduto()               { return idProduto; }
    public void setIdProduto(int i)         { this.idProduto = i; }
    public String getNomeProduto()          { return nomeProduto; }
    public void setNomeProduto(String n)    { this.nomeProduto = n; }
    public int getQuantidade()              { return quantidade; }
    public void setQuantidade(int q)        { this.quantidade = q; }
    public BigDecimal getPrecoUnitario()    { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal p) { this.precoUnitario = p; }
    public BigDecimal getSubtotal()         { return subtotal; }
    public void setSubtotal(BigDecimal s)   { this.subtotal = s; }
}
