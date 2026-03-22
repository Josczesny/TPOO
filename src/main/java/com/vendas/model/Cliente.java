package com.vendas.model;

import java.time.LocalDate;

public class Cliente {
    private int id;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;
    private String endereco;
    private LocalDate dataCadastro;

    public Cliente() {}

    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }
    public String getNome()                 { return nome; }
    public void setNome(String n)           { this.nome = n; }
    public String getCpf()                  { return cpf; }
    public void setCpf(String c)            { this.cpf = c; }
    public String getEmail()                { return email; }
    public void setEmail(String e)          { this.email = e; }
    public String getTelefone()             { return telefone; }
    public void setTelefone(String t)       { this.telefone = t; }
    public String getEndereco()             { return endereco; }
    public void setEndereco(String e)       { this.endereco = e; }
    public LocalDate getDataCadastro()      { return dataCadastro; }
    public void setDataCadastro(LocalDate d){ this.dataCadastro = d; }

    @Override public String toString() { return nome + " (CPF: " + cpf + ")"; }
}
