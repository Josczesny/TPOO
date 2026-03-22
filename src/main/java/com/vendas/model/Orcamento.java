package com.vendas.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Orcamento {
    private int id;
    private String numero;
    private int idCliente;
    private String nomeCliente;
    private LocalDate dataEmissao;
    private LocalDate dataValidade;
    private String status; // PENDENTE, APROVADO, VENCIDO, CANCELADO
    private BigDecimal valorTotal;
    private String observacao;
    private List<ItemOrcamento> itens = new ArrayList<>();

    public Orcamento() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public String getNumero()                   { return numero; }
    public void setNumero(String n)             { this.numero = n; }
    public int getIdCliente()                   { return idCliente; }
    public void setIdCliente(int ic)            { this.idCliente = ic; }
    public String getNomeCliente()              { return nomeCliente; }
    public void setNomeCliente(String nc)       { this.nomeCliente = nc; }
    public LocalDate getDataEmissao()           { return dataEmissao; }
    public void setDataEmissao(LocalDate d)     { this.dataEmissao = d; }
    public LocalDate getDataValidade()          { return dataValidade; }
    public void setDataValidade(LocalDate d)    { this.dataValidade = d; }
    public String getStatus()                   { return status; }
    public void setStatus(String s)             { this.status = s; }
    public BigDecimal getValorTotal()           { return valorTotal; }
    public void setValorTotal(BigDecimal v)     { this.valorTotal = v; }
    public String getObservacao()               { return observacao; }
    public void setObservacao(String o)         { this.observacao = o; }
    public List<ItemOrcamento> getItens()       { return itens; }
    public void setItens(List<ItemOrcamento> i) { this.itens = i; }
}
