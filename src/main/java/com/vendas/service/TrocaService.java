package com.vendas.service;

import com.vendas.dao.ProdutoDAO;
import com.vendas.dao.TrocaDAO;
import com.vendas.dao.VendaDAO;
import com.vendas.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Regras de negócio para Troca:
 * RN01 - A venda deve ter status CONCLUIDA para aceitar troca
 * RN02 - O produto devolvido deve estar presente na venda original
 * RN03 - Quantidade devolvida não pode exceder a quantidade vendida
 * RN04 - O produto novo deve ter estoque disponível
 * RN05 - Calcula diferença de valor (pode ser positivo ou negativo)
 */
public class TrocaService {

    private final TrocaDAO trocaDAO = new TrocaDAO();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    public Troca realizarTroca(Troca troca) {
        Venda venda = vendaDAO.findById(troca.getIdVenda());
        if (venda == null)
            throw new IllegalArgumentException("Venda não encontrada.");

        // RN01
        if (!"CONCLUIDA".equals(venda.getStatus()))
            throw new IllegalArgumentException("Só é possível realizar troca em vendas com status CONCLUIDA. Status: " + venda.getStatus());

        // RN02
        ItemVenda itemOriginal = venda.getItens().stream()
                .filter(iv -> iv.getIdProduto() == troca.getIdProdutoDevolvido())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Produto a ser trocado não encontrado na venda original."));

        // RN03
        if (troca.getQuantidadeDevolvida() > itemOriginal.getQuantidade())
            throw new IllegalArgumentException("Quantidade a trocar (" + troca.getQuantidadeDevolvida() +
                    ") excede a quantidade vendida (" + itemOriginal.getQuantidade() + ").");

        if (troca.getQuantidadeDevolvida() < 1)
            throw new IllegalArgumentException("Quantidade a trocar deve ser maior que zero.");

        // RN04
        Produto produtoNovo = produtoDAO.findById(troca.getIdProdutoNovo());
        if (produtoNovo == null)
            throw new IllegalArgumentException("Produto novo não encontrado.");
        if (produtoNovo.getEstoque() < troca.getQuantidadeNova())
            throw new IllegalArgumentException("Estoque insuficiente para o produto novo: " + produtoNovo.getNome() +
                    " (disponível: " + produtoNovo.getEstoque() + ")");

        if (troca.getQuantidadeNova() < 1)
            throw new IllegalArgumentException("Quantidade do novo produto deve ser maior que zero.");

        // RN05 - calcula diferença de valor
        BigDecimal valorDevolvido = itemOriginal.getPrecoUnitario()
                .multiply(BigDecimal.valueOf(troca.getQuantidadeDevolvida()));
        BigDecimal valorNovo = produtoNovo.getPreco()
                .multiply(BigDecimal.valueOf(troca.getQuantidadeNova()));
        BigDecimal diferenca = valorNovo.subtract(valorDevolvido); // positivo = cliente paga; negativo = loja devolve

        troca.setValorDiferenca(diferenca);
        troca.setDataTroca(LocalDateTime.now());
        troca.setNumero("TRC-" + System.currentTimeMillis());

        trocaDAO.save(troca);

        // Reabastece estoque do produto devolvido
        Produto produtoDevolvido = produtoDAO.findById(troca.getIdProdutoDevolvido());
        produtoDAO.atualizarEstoque(troca.getIdProdutoDevolvido(),
                produtoDevolvido.getEstoque() + troca.getQuantidadeDevolvida());

        // Baixa estoque do produto novo
        produtoDAO.atualizarEstoque(troca.getIdProdutoNovo(),
                produtoNovo.getEstoque() - troca.getQuantidadeNova());

        return troca;
    }
}
