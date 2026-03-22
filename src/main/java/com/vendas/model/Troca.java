package com.vendas.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Troca {
    private int id;
    private String numero;
    private int idVenda;
    private String numeroVenda;
    private String nomeCliente;
    private LocalDateTime dataTroca;
    private String motivo;
    private int idProdutoDevolvido;
    private String nomeProdutoDevolvido;
    private int quantidadeDevolvida;
    private int idProdutoNovo;
    private String nomeProdutoNovo;
    private int quantidadeNova;
    private BigDecimal valorDiferenca;

    public Troca() {}

    public int getId()                           { return id; }
    public void setId(int id)                    { this.id = id; }
    public String getNumero()                    { return numero; }
    public void setNumero(String n)              { this.numero = n; }
    public int getIdVenda()                      { return idVenda; }
    public void setIdVenda(int i)                { this.idVenda = i; }
    public String getNumeroVenda()               { return numeroVenda; }
    public void setNumeroVenda(String n)         { this.numeroVenda = n; }
    public String getNomeCliente()               { return nomeCliente; }
    public void setNomeCliente(String n)         { this.nomeCliente = n; }
    public LocalDateTime getDataTroca()          { return dataTroca; }
    public void setDataTroca(LocalDateTime d)    { this.dataTroca = d; }
    public String getMotivo()                    { return motivo; }
    public void setMotivo(String m)              { this.motivo = m; }
    public int getIdProdutoDevolvido()           { return idProdutoDevolvido; }
    public void setIdProdutoDevolvido(int i)     { this.idProdutoDevolvido = i; }
    public String getNomeProdutoDevolvido()      { return nomeProdutoDevolvido; }
    public void setNomeProdutoDevolvido(String n){ this.nomeProdutoDevolvido = n; }
    public int getQuantidadeDevolvida()          { return quantidadeDevolvida; }
    public void setQuantidadeDevolvida(int q)    { this.quantidadeDevolvida = q; }
    public int getIdProdutoNovo()                { return idProdutoNovo; }
    public void setIdProdutoNovo(int i)          { this.idProdutoNovo = i; }
    public String getNomeProdutoNovo()           { return nomeProdutoNovo; }
    public void setNomeProdutoNovo(String n)     { this.nomeProdutoNovo = n; }
    public int getQuantidadeNova()               { return quantidadeNova; }
    public void setQuantidadeNova(int q)         { this.quantidadeNova = q; }
    public BigDecimal getValorDiferenca()        { return valorDiferenca; }
    public void setValorDiferenca(BigDecimal v)  { this.valorDiferenca = v; }
}
