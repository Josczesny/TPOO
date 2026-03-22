package com.vendas.ui;

import com.vendas.dao.ClienteDAO;
import com.vendas.dao.OrcamentoDAO;
import com.vendas.dao.ProdutoDAO;
import com.vendas.model.*;
import com.vendas.service.OrcamentoService;
import com.vendas.service.VendaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrcamentoPanel extends JPanel {

    private final OrcamentoDAO dao = new OrcamentoDAO();
    private final OrcamentoService service = new OrcamentoService();
    private final VendaService vendaService = new VendaService();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public OrcamentoPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNovo      = new JButton("Novo Orçamento");
        JButton btnDetalhes  = new JButton("Ver Detalhes");
        JButton btnConverter = new JButton("Converter em Venda");
        JButton btnCancelar  = new JButton("Cancelar Orçamento");
        JButton btnAtualizar = new JButton("Atualizar");
        topPanel.add(btnNovo); topPanel.add(btnDetalhes);
        topPanel.add(btnConverter); topPanel.add(btnCancelar); topPanel.add(btnAtualizar);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Número", "Cliente", "Emissão", "Validade", "Status", "Total"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnAtualizar.addActionListener(e -> { service.atualizarVencidos(); carregar(); });
        btnNovo.addActionListener(e -> abrirNovoOrcamento());
        btnDetalhes.addActionListener(e -> verDetalhes());
        btnConverter.addActionListener(e -> converterEmVenda());
        btnCancelar.addActionListener(e -> cancelarOrcamento());

        service.atualizarVencidos();
        carregar();
    }

    private void carregar() {
        tableModel.setRowCount(0);
        dao.findAll().forEach(o -> tableModel.addRow(new Object[]{
            o.getId(), o.getNumero(), o.getNomeCliente(),
            o.getDataEmissao(), o.getDataValidade(), o.getStatus(),
            CURRENCY.format(o.getValorTotal())
        }));
    }

    private Orcamento getSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione um orçamento."); return null; }
        return dao.findById((int) tableModel.getValueAt(row, 0));
    }

    private void abrirNovoOrcamento() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Novo Orçamento", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(750, 550);
        dialog.setLocationRelativeTo(this);

        ClienteDAO clienteDAO = new ClienteDAO();
        ProdutoDAO produtoDAO = new ProdutoDAO();

        // Painel superior: seleção de cliente
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<Cliente> cmbCliente = new JComboBox<>();
        clienteDAO.findAll().forEach(cmbCliente::addItem);
        JTextField txtObs = new JTextField(20);
        headerPanel.add(new JLabel("Cliente*:"));
        headerPanel.add(cmbCliente);
        headerPanel.add(new JLabel("Observação:"));
        headerPanel.add(txtObs);

        // Tabela de itens
        DefaultTableModel itensModel = new DefaultTableModel(
                new String[]{"ID Produto", "Produto", "Preço Unit.", "Qtd", "Subtotal"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable itensTable = new JTable(itensModel);
        itensTable.getColumnModel().getColumn(0).setMaxWidth(0);
        itensTable.getColumnModel().getColumn(0).setMinWidth(0);
        TableCellRenderer currencyRenderer = (tbl, value, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(value instanceof BigDecimal ? CURRENCY.format(value) : String.valueOf(value));
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            if (sel) { lbl.setOpaque(true); lbl.setBackground(tbl.getSelectionBackground()); }
            return lbl;
        };
        itensTable.getColumnModel().getColumn(2).setCellRenderer(currencyRenderer);
        itensTable.getColumnModel().getColumn(4).setCellRenderer(currencyRenderer);

        // Painel adicionar item
        JPanel addItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<Produto> cmbProduto = new JComboBox<>();
        produtoDAO.findAll().forEach(cmbProduto::addItem);
        JTextField txtQtd = new JTextField("1", 5);
        JButton btnAdd = new JButton("Adicionar Item");
        JButton btnRemove = new JButton("Remover Item");
        addItemPanel.add(new JLabel("Produto:"));
        addItemPanel.add(cmbProduto);
        addItemPanel.add(new JLabel("Qtd:"));
        addItemPanel.add(txtQtd);
        addItemPanel.add(btnAdd);
        addItemPanel.add(btnRemove);

        JLabel lblTotal = new JLabel("Total: R$ 0,00");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 14f));

        Runnable calcTotal = () -> {
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < itensModel.getRowCount(); i++) {
                Object val = itensModel.getValueAt(i, 4);
                if (val instanceof BigDecimal) total = total.add((BigDecimal) val);
            }
            lblTotal.setText("Total: " + CURRENCY.format(total));
        };

        btnAdd.addActionListener(e -> {
            Produto p = (Produto) cmbProduto.getSelectedItem();
            if (p == null) return;
            try {
                int qtd = Integer.parseInt(txtQtd.getText().trim());
                if (qtd <= 0) throw new NumberFormatException();
                BigDecimal subtotal = p.getPreco().multiply(BigDecimal.valueOf(qtd));
                itensModel.addRow(new Object[]{p.getId(), p.toString(), p.getPreco(), qtd, subtotal});
                calcTotal.run();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Quantidade inválida.");
            }
        });
        btnRemove.addActionListener(e -> {
            int row = itensTable.getSelectedRow();
            if (row >= 0) { itensModel.removeRow(row); calcTotal.run(); }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvar = new JButton("Salvar Orçamento");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(lblTotal); btnPanel.add(btnSalvar); btnPanel.add(btnCancel);

        JPanel center = new JPanel(new BorderLayout());
        center.add(new JScrollPane(itensTable), BorderLayout.CENTER);
        center.add(addItemPanel, BorderLayout.SOUTH);

        dialog.setLayout(new BorderLayout(5, 5));
        dialog.add(headerPanel, BorderLayout.NORTH);
        dialog.add(center, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSalvar.addActionListener(e -> {
            try {
                Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
                if (cliente == null) throw new IllegalArgumentException("Selecione um cliente.");
                if (itensModel.getRowCount() == 0) throw new IllegalArgumentException("Adicione pelo menos 1 item.");

                Orcamento orc = new Orcamento();
                orc.setIdCliente(cliente.getId());
                orc.setObservacao(txtObs.getText().trim());

                List<ItemOrcamento> itens = new ArrayList<>();
                for (int i = 0; i < itensModel.getRowCount(); i++) {
                    ItemOrcamento item = new ItemOrcamento();
                    item.setIdProduto((int) itensModel.getValueAt(i, 0));
                    item.setQuantidade((int) itensModel.getValueAt(i, 3));
                    itens.add(item);
                }
                orc.setItens(itens);

                service.criarOrcamento(orc);
                JOptionPane.showMessageDialog(dialog, "Orçamento " + orc.getNumero() + " criado com sucesso!\nVálido até: " + orc.getDataValidade());
                dialog.dispose();
                carregar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void verDetalhes() {
        Orcamento o = getSelecionado();
        if (o == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Orçamento: ").append(o.getNumero()).append("\n");
        sb.append("Cliente: ").append(o.getNomeCliente()).append("\n");
        sb.append("Emissão: ").append(o.getDataEmissao()).append("   Validade: ").append(o.getDataValidade()).append("\n");
        sb.append("Status: ").append(o.getStatus()).append("\n\n");
        sb.append("ITENS:\n");
        for (ItemOrcamento item : o.getItens()) {
            sb.append(String.format("  - %s: %d x %s = %s%n",
                    item.getNomeProduto(), item.getQuantidade(),
                    CURRENCY.format(item.getPrecoUnitario()), CURRENCY.format(item.getSubtotal())));
        }
        sb.append("\nTOTAL: ").append(CURRENCY.format(o.getValorTotal()));
        JOptionPane.showMessageDialog(this, sb.toString(), "Detalhes do Orçamento", JOptionPane.INFORMATION_MESSAGE);
    }

    private void converterEmVenda() {
        Orcamento o = getSelecionado();
        if (o == null) return;

        String[] pagamentos = {"Dinheiro", "Cartão de Crédito", "Cartão de Débito", "PIX", "Boleto"};
        JComboBox<String> cmbPag = new JComboBox<>(pagamentos);
        JTextField txtDesconto = new JTextField("0.00", 10);
        JTextField txtObs = new JTextField(20);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Forma de Pagamento:")); panel.add(cmbPag);
        panel.add(new JLabel("Desconto (R$):")); panel.add(txtDesconto);
        panel.add(new JLabel("Observação:")); panel.add(txtObs);

        int opt = JOptionPane.showConfirmDialog(this, panel, "Converter em Venda", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                BigDecimal desconto = new BigDecimal(txtDesconto.getText().trim().replace(",", "."));
                Venda v = vendaService.converterOrcamentoEmVenda(o.getId(),
                        (String) cmbPag.getSelectedItem(), desconto, txtObs.getText().trim());
                JOptionPane.showMessageDialog(this, "Venda " + v.getNumero() + " criada com sucesso!");
                carregar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cancelarOrcamento() {
        Orcamento o = getSelecionado();
        if (o == null) return;
        int conf = JOptionPane.showConfirmDialog(this, "Cancelar orçamento " + o.getNumero() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf == JOptionPane.YES_OPTION) {
            try { service.cancelarOrcamento(o.getId()); carregar(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE); }
        }
    }
}
