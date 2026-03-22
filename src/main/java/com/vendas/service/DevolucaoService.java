package com.vendas.service;

import com.vendas.dao.DevolucaoDAO;
import com.vendas.dao.ProdutoDAO;
import com.vendas.dao.VendaDAO;
import com.vendas.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Regras de negócio para Devolução:
 * RN01 - Só é possível devolver vendas realizadas nos últimos 30 dias
 * RN02 - A venda deve ter status CONCLUIDA para aceitar devolução
 * RN03 - Quantidade devolvida não pode exceder quantidade vendida
 * RN04 - Ao registrar devolução, o estoque dos produtos é reabastecido
 */
public class DevolucaoService {

    private final DevolucaoDAO devolucaoDAO = new DevolucaoDAO();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    public Devolucao realizarDevolucao(Devolucao devolucao) {
        Venda venda = vendaDAO.findById(devolucao.getIdVenda());
        if (venda == null)
            throw new IllegalArgumentException("Venda não encontrada.");

        // RN02
        if (!"CONCLUIDA".equals(venda.getStatus()))
            throw new IllegalArgumentException("Só é possível devolver vendas com status CONCLUIDA. Status atual: " + venda.getStatus());

        // RN01
        if (venda.getDataVenda().isBefore(LocalDateTime.now().minusDays(30)))
            throw new IllegalArgumentException("Devolução não permitida. A venda foi realizada há mais de 30 dias (" +
                    venda.getDataVenda().toLocalDate() + ").");

        if (devolucao.getItens() == null || devolucao.getItens().isEmpty())
            throw new IllegalArgumentException("A devolução deve ter pelo menos 1 item.");

        BigDecimal totalDevolvido = BigDecimal.ZERO;
        for (ItemDevolucao itemDev : devolucao.getItens()) {
            // RN03 - verifica se a quantidade devolvida não excede a vendida
            ItemVenda itemVenda = venda.getItens().stream()
                    .filter(iv -> iv.getIdProduto() == itemDev.getIdProduto())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Produto ID " + itemDev.getIdProduto() + " não encontrado na venda original."));

            if (itemDev.getQuantidade() > itemVenda.getQuantidade())
                throw new IllegalArgumentException("Quantidade devolvida (" + itemDev.getQuantidade() +
                        ") não pode ser maior que a quantidade vendida (" + itemVenda.getQuantidade() +
                        ") para o produto: " + itemVenda.getNomeProduto());

            itemDev.setPrecoUnitario(itemVenda.getPrecoUnitario());
            itemDev.setSubtotal(itemVenda.getPrecoUnitario().multiply(BigDecimal.valueOf(itemDev.getQuantidade())));
            totalDevolvido = totalDevolvido.add(itemDev.getSubtotal());
        }

        devolucao.setValorDevolvido(totalDevolvido);
        devolucao.setDataDevolucao(LocalDateTime.now());
        devolucao.setNumero("DEV-" + System.currentTimeMillis());

        devolucaoDAO.save(devolucao);

        // RN04 - reabastece estoque
        for (ItemDevolucao item : devolucao.getItens()) {
            Produto produto = produtoDAO.findById(item.getIdProduto());
            produtoDAO.atualizarEstoque(item.getIdProduto(), produto.getEstoque() + item.getQuantidade());
        }

        // Atualiza status da venda
        vendaDAO.updateStatus(venda.getId(), "DEVOLVIDA");

        return devolucao;
    }
}
