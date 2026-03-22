package com.vendas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Devolucao {
    private int id;
    private String numero;
    private int idVenda;
    private String numeroVenda;
    private String nomeCliente;
    private LocalDateTime dataDevolucao;
    private String motivo;
    private BigDecimal valorDevolvido;
    private List<ItemDevolucao> itens = new ArrayList<>();

    public Devolucao() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public String getNumero()                   { return numero; }
    public void setNumero(String n)             { this.numero = n; }
    public int getIdVenda()                     { return idVenda; }
    public void setIdVenda(int i)               { this.idVenda = i; }
    public String getNumeroVenda()              { return numeroVenda; }
    public void setNumeroVenda(String n)        { this.numeroVenda = n; }
    public String getNomeCliente()              { return nomeCliente; }
    public void setNomeCliente(String n)        { this.nomeCliente = n; }
    public LocalDateTime getDataDevolucao()     { return dataDevolucao; }
    public void setDataDevolucao(LocalDateTime d){ this.dataDevolucao = d; }
    public String getMotivo()                   { return motivo; }
    public void setMotivo(String m)             { this.motivo = m; }
    public BigDecimal getValorDevolvido()       { return valorDevolvido; }
    public void setValorDevolvido(BigDecimal v) { this.valorDevolvido = v; }
    public List<ItemDevolucao> getItens()       { return itens; }
    public void setItens(List<ItemDevolucao> i) { this.itens = i; }
}
