package com.vendas.service;

import com.vendas.dao.OrcamentoDAO;
import com.vendas.dao.ProdutoDAO;
import com.vendas.dao.VendaDAO;
import com.vendas.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Regras de negócio para Venda:
 * RN01 - Venda deve ter pelo menos 1 item
 * RN02 - Estoque deve ser suficiente para cada produto vendido
 * RN03 - Desconto não pode ser maior que o valor total
 * RN04 - Ao converter orçamento, ele deve estar com status PENDENTE e dentro da validade
 * RN05 - Após a venda, estoque dos produtos é reduzido
 */
public class VendaService {

    private final VendaDAO vendaDAO = new VendaDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final OrcamentoDAO orcamentoDAO = new OrcamentoDAO();

    public Venda efetuarVenda(Venda venda) {
        // RN01
        if (venda.getItens() == null || venda.getItens().isEmpty())
            throw new IllegalArgumentException("A venda deve ter pelo menos 1 item.");

        BigDecimal total = BigDecimal.ZERO;
        for (ItemVenda item : venda.getItens()) {
            Produto produto = produtoDAO.findById(item.getIdProduto());
            if (produto == null)
                throw new IllegalArgumentException("Produto não encontrado: ID " + item.getIdProduto());

            // RN02
            if (produto.getEstoque() < item.getQuantidade())
                throw new IllegalArgumentException("Estoque insuficiente para: " + produto.getNome() +
                        " (disponível: " + produto.getEstoque() + ", solicitado: " + item.getQuantidade() + ")");

            item.setPrecoUnitario(produto.getPreco());
            item.setSubtotal(produto.getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())));
            total = total.add(item.getSubtotal());
        }

        venda.setValorTotal(total);

        // RN03
        if (venda.getDesconto() == null) venda.setDesconto(BigDecimal.ZERO);
        if (venda.getDesconto().compareTo(total) > 0)
            throw new IllegalArgumentException("O desconto não pode ser maior que o valor total da venda.");

        venda.setValorFinal(total.subtract(venda.getDesconto()));
        venda.setDataVenda(LocalDateTime.now());
        venda.setStatus("CONCLUIDA");
        venda.setNumero("VND-" + System.currentTimeMillis());

        vendaDAO.save(venda);

        // RN05 - atualizar estoque
        for (ItemVenda item : venda.getItens()) {
            Produto produto = produtoDAO.findById(item.getIdProduto());
            produtoDAO.atualizarEstoque(item.getIdProduto(), produto.getEstoque() - item.getQuantidade());
        }

        // Marcar orçamento como aprovado se veio de orçamento
        if (venda.getIdOrcamento() != null) {
            orcamentoDAO.updateStatus(venda.getIdOrcamento(), "APROVADO");
        }

        return venda;
    }

    public Venda converterOrcamentoEmVenda(int idOrcamento, String formaPagamento, BigDecimal desconto, String observacao) {
        Orcamento orc = orcamentoDAO.findById(idOrcamento);
        if (orc == null)
            throw new IllegalArgumentException("Orçamento não encontrado.");

        // RN04
        if (!"PENDENTE".equals(orc.getStatus()))
            throw new IllegalArgumentException("Orçamento não está mais pendente. Status: " + orc.getStatus());
        if (orc.getDataValidade().isBefore(java.time.LocalDate.now()))
            throw new IllegalArgumentException("Orçamento vencido em " + orc.getDataValidade() + ". Não é possível convertê-lo.");

        Venda venda = new Venda();
        venda.setIdCliente(orc.getIdCliente());
        venda.setIdOrcamento(orc.getId());
        venda.setFormaPagamento(formaPagamento);
        venda.setDesconto(desconto != null ? desconto : BigDecimal.ZERO);
        venda.setObservacao(observacao);

        for (ItemOrcamento io : orc.getItens()) {
            ItemVenda iv = new ItemVenda();
            iv.setIdProduto(io.getIdProduto());
            iv.setNomeProduto(io.getNomeProduto());
            iv.setQuantidade(io.getQuantidade());
            iv.setPrecoUnitario(io.getPrecoUnitario());
            iv.setSubtotal(io.getSubtotal());
            venda.getItens().add(iv);
        }

        return efetuarVenda(venda);
    }
}
