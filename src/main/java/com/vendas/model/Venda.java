package com.vendas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venda {
    private int id;
    private String numero;
    private int idCliente;
    private String nomeCliente;
    private Integer idOrcamento;
    private LocalDateTime dataVenda;
    private String formaPagamento;
    private BigDecimal valorTotal;
    private BigDecimal desconto;
    private BigDecimal valorFinal;
    private String status; // CONCLUIDA, DEVOLVIDA
    private String observacao;
    private List<ItemVenda> itens = new ArrayList<>();

    public Venda() {}

    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public String getNumero()                 { return numero; }
    public void setNumero(String n)           { this.numero = n; }
    public int getIdCliente()                 { return idCliente; }
    public void setIdCliente(int ic)          { this.idCliente = ic; }
    public String getNomeCliente()            { return nomeCliente; }
    public void setNomeCliente(String nc)     { this.nomeCliente = nc; }
    public Integer getIdOrcamento()           { return idOrcamento; }
    public void setIdOrcamento(Integer io)    { this.idOrcamento = io; }
    public LocalDateTime getDataVenda()       { return dataVenda; }
    public void setDataVenda(LocalDateTime d) { this.dataVenda = d; }
    public String getFormaPagamento()         { return formaPagamento; }
    public void setFormaPagamento(String f)   { this.formaPagamento = f; }
    public BigDecimal getValorTotal()         { return valorTotal; }
    public void setValorTotal(BigDecimal v)   { this.valorTotal = v; }
    public BigDecimal getDesconto()           { return desconto; }
    public void setDesconto(BigDecimal d)     { this.desconto = d; }
    public BigDecimal getValorFinal()         { return valorFinal; }
    public void setValorFinal(BigDecimal v)   { this.valorFinal = v; }
    public String getStatus()                 { return status; }
    public void setStatus(String s)           { this.status = s; }
    public String getObservacao()             { return observacao; }
    public void setObservacao(String o)       { this.observacao = o; }
    public List<ItemVenda> getItens()         { return itens; }
    public void setItens(List<ItemVenda> i)   { this.itens = i; }
}
