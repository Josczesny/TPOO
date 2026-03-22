package com.vendas.service;

import com.vendas.dao.OrcamentoDAO;
import com.vendas.dao.ProdutoDAO;
import com.vendas.model.ItemOrcamento;
import com.vendas.model.Orcamento;
import com.vendas.model.Produto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Regras de negócio para Orçamento:
 * RN01 - Orçamento deve ter pelo menos 1 item
 * RN02 - Quantidade de cada item deve ser >= 1
 * RN03 - Produto deve ter estoque suficiente para o orçamento
 * RN04 - Validade do orçamento é de 7 dias a partir da emissão
 * RN05 - Só é possível cancelar orçamento com status PENDENTE
 */
public class OrcamentoService {

    private final OrcamentoDAO orcamentoDAO = new OrcamentoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    public Orcamento criarOrcamento(Orcamento orcamento) {
        // RN01
        if (orcamento.getItens() == null || orcamento.getItens().isEmpty())
            throw new IllegalArgumentException("O orçamento deve ter pelo menos 1 item.");

        for (ItemOrcamento item : orcamento.getItens()) {
            // RN02
            if (item.getQuantidade() < 1)
                throw new IllegalArgumentException("A quantidade do item deve ser maior que zero.");

            // RN03
            Produto produto = produtoDAO.findById(item.getIdProduto());
            if (produto == null)
                throw new IllegalArgumentException("Produto não encontrado: ID " + item.getIdProduto());
            if (produto.getEstoque() < item.getQuantidade())
                throw new IllegalArgumentException("Estoque insuficiente para o produto: " + produto.getNome() +
                        " (disponível: " + produto.getEstoque() + ")");

            item.setPrecoUnitario(produto.getPreco());
            item.setSubtotal(produto.getPreco().multiply(BigDecimal.valueOf(item.getQuantidade())));
        }

        // RN04 - validade de 7 dias
        orcamento.setDataEmissao(LocalDate.now());
        orcamento.setDataValidade(LocalDate.now().plusDays(7));
        orcamento.setStatus("PENDENTE");

        BigDecimal total = orcamento.getItens().stream()
                .map(ItemOrcamento::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orcamento.setValorTotal(total);

        // Gera número sequencial
        orcamento.setNumero("ORC-" + System.currentTimeMillis());

        orcamentoDAO.save(orcamento);
        return orcamento;
    }

    public void cancelarOrcamento(int id) {
        Orcamento o = orcamentoDAO.findById(id);
        if (o == null)
            throw new IllegalArgumentException("Orçamento não encontrado.");
        // RN05
        if (!"PENDENTE".equals(o.getStatus()))
            throw new IllegalArgumentException("Apenas orçamentos PENDENTES podem ser cancelados. Status atual: " + o.getStatus());
        orcamentoDAO.updateStatus(id, "CANCELADO");
    }

    public void atualizarVencidos() {
        orcamentoDAO.atualizarOrcamentosVencidos();
    }
}
