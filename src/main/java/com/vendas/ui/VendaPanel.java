package com.vendas.ui;

import com.vendas.dao.ClienteDAO;
import com.vendas.dao.ProdutoDAO;
import com.vendas.dao.VendaDAO;
import com.vendas.model.*;
import com.vendas.service.VendaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VendaPanel extends JPanel {

    private final VendaDAO dao = new VendaDAO();
    private final VendaService service = new VendaService();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public VendaPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNova      = new JButton("Nova Venda");
        JButton btnDetalhes  = new JButton("Ver Detalhes");
        JButton btnAtualizar = new JButton("Atualizar");
        topPanel.add(btnNova); topPanel.add(btnDetalhes); topPanel.add(btnAtualizar);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Número", "Cliente", "Data/Hora", "Pagamento", "Total", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnNova.addActionListener(e -> abrirNovaVenda());
        btnDetalhes.addActionListener(e -> verDetalhes());
        btnAtualizar.addActionListener(e -> carregar());

        carregar();
    }

    private void carregar() {
        tableModel.setRowCount(0);
        dao.findAll().forEach(v -> tableModel.addRow(new Object[]{
            v.getId(), v.getNumero(), v.getNomeCliente(),
            v.getDataVenda().format(FMT), v.getFormaPagamento(),
            CURRENCY.format(v.getValorFinal()), v.getStatus()
        }));
    }

    private Venda getSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma venda."); return null; }
        return dao.findById((int) tableModel.getValueAt(row, 0));
    }

    private void abrirNovaVenda() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Nova Venda", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        ClienteDAO clienteDAO = new ClienteDAO();
        ProdutoDAO produtoDAO = new ProdutoDAO();

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<Cliente> cmbCliente = new JComboBox<>();
        clienteDAO.findAll().forEach(cmbCliente::addItem);
        String[] pagamentos = {"Dinheiro", "Cartão de Crédito", "Cartão de Débito", "PIX", "Boleto"};
        JComboBox<String> cmbPag = new JComboBox<>(pagamentos);
        JTextField txtDesconto = new JTextField("0.00", 8);
        JTextField txtObs = new JTextField(15);
        headerPanel.add(new JLabel("Cliente*:")); headerPanel.add(cmbCliente);
        headerPanel.add(new JLabel("Pagamento:")); headerPanel.add(cmbPag);
        headerPanel.add(new JLabel("Desconto R$:")); headerPanel.add(txtDesconto);
        headerPanel.add(new JLabel("Obs:")); headerPanel.add(txtObs);

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

        // Adicionar item
        JPanel addItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<Produto> cmbProduto = new JComboBox<>();
        produtoDAO.findAll().forEach(cmbProduto::addItem);
        JTextField txtQtd = new JTextField("1", 5);
        JButton btnAdd    = new JButton("Adicionar");
        JButton btnRemove = new JButton("Remover");
        addItemPanel.add(new JLabel("Produto:")); addItemPanel.add(cmbProduto);
        addItemPanel.add(new JLabel("Qtd:")); addItemPanel.add(txtQtd);
        addItemPanel.add(btnAdd); addItemPanel.add(btnRemove);

        JLabel lblTotal = new JLabel("Total: R$ 0,00   |   Final: R$ 0,00");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 13f));

        Runnable calcTotal = () -> {
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < itensModel.getRowCount(); i++) {
                Object val = itensModel.getValueAt(i, 4);
                if (val instanceof BigDecimal) total = total.add((BigDecimal) val);
            }
            BigDecimal desc;
            try { desc = new BigDecimal(txtDesconto.getText().replace(",", ".")); }
            catch (Exception e) { desc = BigDecimal.ZERO; }
            BigDecimal final_ = total.subtract(desc.max(BigDecimal.ZERO));
            lblTotal.setText("Total: " + CURRENCY.format(total) + "   |   Final (c/ desconto): " + CURRENCY.format(final_));
        };

        btnAdd.addActionListener(e -> {
            Produto p = (Produto) cmbProduto.getSelectedItem();
            if (p == null) return;
            try {
                int qtd = Integer.parseInt(txtQtd.getText().trim());
                if (qtd <= 0) throw new NumberFormatException();
                if (qtd > p.getEstoque()) {
                    JOptionPane.showMessageDialog(dialog, "Estoque insuficiente! Disponível: " + p.getEstoque());
                    return;
                }
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
        txtDesconto.addActionListener(e -> calcTotal.run());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvar = new JButton("Finalizar Venda");
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

                BigDecimal desconto;
                try { desconto = new BigDecimal(txtDesconto.getText().trim().replace(",", ".")); }
                catch (Exception ex) { desconto = BigDecimal.ZERO; }

                Venda venda = new Venda();
                venda.setIdCliente(cliente.getId());
                venda.setFormaPagamento((String) cmbPag.getSelectedItem());
                venda.setDesconto(desconto);
                venda.setObservacao(txtObs.getText().trim());

                List<ItemVenda> itens = new ArrayList<>();
                for (int i = 0; i < itensModel.getRowCount(); i++) {
                    ItemVenda item = new ItemVenda();
                    item.setIdProduto((int) itensModel.getValueAt(i, 0));
                    item.setQuantidade((int) itensModel.getValueAt(i, 3));
                    itens.add(item);
                }
                venda.setItens(itens);

                service.efetuarVenda(venda);
                JOptionPane.showMessageDialog(dialog, "Venda " + venda.getNumero() + " realizada!\nValor Final: " + CURRENCY.format(venda.getValorFinal()));
                dialog.dispose();
                carregar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void verDetalhes() {
        Venda v = getSelecionado();
        if (v == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Venda: ").append(v.getNumero()).append("\n");
        sb.append("Cliente: ").append(v.getNomeCliente()).append("\n");
        sb.append("Data/Hora: ").append(v.getDataVenda().format(FMT)).append("\n");
        sb.append("Pagamento: ").append(v.getFormaPagamento()).append("\n");
        sb.append("Status: ").append(v.getStatus()).append("\n\n");
        sb.append("ITENS:\n");
        for (ItemVenda item : v.getItens()) {
            sb.append(String.format("  - %s: %d x %s = %s%n",
                    item.getNomeProduto(), item.getQuantidade(),
                    CURRENCY.format(item.getPrecoUnitario()), CURRENCY.format(item.getSubtotal())));
        }
        sb.append("\nTotal: ").append(CURRENCY.format(v.getValorTotal()));
        sb.append("\nDesconto: ").append(CURRENCY.format(v.getDesconto()));
        sb.append("\nValor Final: ").append(CURRENCY.format(v.getValorFinal()));
        if (v.getObservacao() != null && !v.getObservacao().isEmpty())
            sb.append("\nObs: ").append(v.getObservacao());
        JOptionPane.showMessageDialog(this, sb.toString(), "Detalhes da Venda", JOptionPane.INFORMATION_MESSAGE);
    }
}
