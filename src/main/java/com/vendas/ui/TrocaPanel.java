package com.vendas.ui;

import com.vendas.dao.ProdutoDAO;
import com.vendas.dao.TrocaDAO;
import com.vendas.dao.VendaDAO;
import com.vendas.model.*;
import com.vendas.service.TrocaService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TrocaPanel extends JPanel {

    private final TrocaDAO dao = new TrocaDAO();
    private final TrocaService service = new TrocaService();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TrocaPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNova      = new JButton("Nova Troca");
        JButton btnDetalhes  = new JButton("Ver Detalhes");
        JButton btnAtualizar = new JButton("Atualizar");
        topPanel.add(btnNova); topPanel.add(btnDetalhes); topPanel.add(btnAtualizar);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Número", "Nº Venda", "Cliente", "Data", "Prod. Devolvido", "Prod. Novo", "Diferença"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnNova.addActionListener(e -> abrirNovaTroca());
        btnDetalhes.addActionListener(e -> verDetalhes());
        btnAtualizar.addActionListener(e -> carregar());

        carregar();
    }

    private void carregar() {
        tableModel.setRowCount(0);
        dao.findAll().forEach(t -> tableModel.addRow(new Object[]{
            t.getId(), t.getNumero(), t.getNumeroVenda(), t.getNomeCliente(),
            t.getDataTroca().format(FMT),
            t.getNomeProdutoDevolvido() + " (x" + t.getQuantidadeDevolvida() + ")",
            t.getNomeProdutoNovo() + " (x" + t.getQuantidadeNova() + ")",
            CURRENCY.format(t.getValorDiferenca())
        }));
    }

    private Troca getSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma troca."); return null; }
        return dao.findById((int) tableModel.getValueAt(row, 0));
    }

    private void abrirNovaTroca() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Nova Troca", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(5, 5));

        // Busca venda
        JPanel buscaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtNumVenda = new JTextField(15);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnSelecionar = new JButton("Selecionar Venda...");
        JLabel lblInfo = new JLabel("  Selecione uma venda ou digite o número");
        lblInfo.setForeground(Color.GRAY);
        buscaPanel.add(new JLabel("Nº Venda:")); buscaPanel.add(txtNumVenda);
        buscaPanel.add(btnBuscar); buscaPanel.add(btnSelecionar); buscaPanel.add(lblInfo);

        // Formulário
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JComboBox<ItemVenda> cmbProdDev = new JComboBox<>();
        cmbProdDev.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ItemVenda iv)
                    setText(iv.getNomeProduto() + " (qtd vendida: " + iv.getQuantidade() + ")");
                return this;
            }
        });

        JTextField txtQtdDev  = new JTextField("1", 6);
        JComboBox<Produto> cmbProdNovo = new JComboBox<>();
        produtoDAO.findAll().forEach(cmbProdNovo::addItem);
        JTextField txtQtdNova = new JTextField("1", 6);
        JTextField txtMotivo  = new JTextField(30);
        JLabel lblDiferenca   = new JLabel("Diferença: ---");
        lblDiferenca.setFont(lblDiferenca.getFont().deriveFont(Font.BOLD));

        Runnable calcDif = () -> {
            ItemVenda iv = (ItemVenda) cmbProdDev.getSelectedItem();
            Produto pNovo = (Produto) cmbProdNovo.getSelectedItem();
            if (iv == null || pNovo == null) return;
            try {
                int qdDev  = Integer.parseInt(txtQtdDev.getText().trim());
                int qdNova = Integer.parseInt(txtQtdNova.getText().trim());
                BigDecimal valDev  = iv.getPrecoUnitario().multiply(BigDecimal.valueOf(qdDev));
                BigDecimal valNovo = pNovo.getPreco().multiply(BigDecimal.valueOf(qdNova));
                BigDecimal dif = valNovo.subtract(valDev);
                String label = dif.compareTo(BigDecimal.ZERO) > 0
                        ? "Cliente paga a diferença: " + CURRENCY.format(dif)
                        : dif.compareTo(BigDecimal.ZERO) < 0
                        ? "Loja devolve: " + CURRENCY.format(dif.abs())
                        : "Sem diferença de valor";
                lblDiferenca.setText(label);
            } catch (Exception ignored) {}
        };
        cmbProdDev.addActionListener(e -> calcDif.run());
        cmbProdNovo.addActionListener(e -> calcDif.run());
        txtQtdDev.addActionListener(e -> calcDif.run());
        txtQtdNova.addActionListener(e -> calcDif.run());

        // Monta formulário
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; formPanel.add(new JLabel("Produto a Devolver*:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; formPanel.add(cmbProdDev, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; formPanel.add(new JLabel("Qtd a Devolver*:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; formPanel.add(txtQtdDev, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; formPanel.add(new JLabel("Produto Novo*:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; formPanel.add(cmbProdNovo, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; formPanel.add(new JLabel("Qtd do Novo*:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; formPanel.add(txtQtdNova, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; formPanel.add(new JLabel("Motivo*:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; formPanel.add(txtMotivo, gbc);
        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; formPanel.add(lblDiferenca, gbc);
        gbc.gridwidth = 1;

        final Venda[] vendaRef = {null};

        Runnable carregarVenda = () -> {
            String num = txtNumVenda.getText().trim();
            if (num.isEmpty()) return;
            Venda v = vendaDAO.findByNumero(num);
            if (v == null) {
                lblInfo.setText("  Venda não encontrada: " + num);
                lblInfo.setForeground(Color.RED);
                vendaRef[0] = null;
                cmbProdDev.removeAllItems();
                return;
            }
            vendaRef[0] = v;
            lblInfo.setText("  " + v.getNomeCliente() + " | " + v.getDataVenda().format(FMT) + " | Status: " + v.getStatus());
            lblInfo.setForeground(new Color(0, 128, 0));
            cmbProdDev.removeAllItems();
            v.getItens().forEach(cmbProdDev::addItem);
        };

        btnBuscar.addActionListener(e -> {
            if (txtNumVenda.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(dialog, "Digite o número da venda."); return; }
            carregarVenda.run();
        });

        btnSelecionar.addActionListener(e -> {
            List<Venda> vendas = vendaDAO.findAll().stream()
                    .filter(v -> "CONCLUIDA".equals(v.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
            if (vendas.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Nenhuma venda com status CONCLUIDA disponível para troca.");
                return;
            }
            DefaultTableModel listaModel = new DefaultTableModel(
                    new String[]{"Número", "Cliente", "Data", "Valor Final"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            vendas.forEach(v -> listaModel.addRow(new Object[]{
                v.getNumero(), v.getNomeCliente(), v.getDataVenda().format(FMT), CURRENCY.format(v.getValorFinal())
            }));
            JTable listaTable = new JTable(listaModel);
            listaTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JDialog selDialog = new JDialog(dialog, "Selecionar Venda", true);
            selDialog.setSize(620, 350);
            selDialog.setLocationRelativeTo(dialog);
            selDialog.setLayout(new BorderLayout(5, 5));
            selDialog.add(new JScrollPane(listaTable), BorderLayout.CENTER);
            JButton btnOk = new JButton("Selecionar");
            JButton btnCancelar = new JButton("Cancelar");
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            p.add(btnOk); p.add(btnCancelar);
            selDialog.add(p, BorderLayout.SOUTH);
            btnCancelar.addActionListener(ev -> selDialog.dispose());
            btnOk.addActionListener(ev -> {
                int r = listaTable.getSelectedRow();
                if (r < 0) { JOptionPane.showMessageDialog(selDialog, "Selecione uma venda."); return; }
                txtNumVenda.setText((String) listaModel.getValueAt(r, 0));
                selDialog.dispose();
                carregarVenda.run();
            });
            listaTable.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent ev) {
                    if (ev.getClickCount() == 2) btnOk.doClick();
                }
            });
            selDialog.setVisible(true);
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvar = new JButton("Registrar Troca");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSalvar); btnPanel.add(btnCancel);

        dialog.add(buscaPanel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(formPanel), BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSalvar.addActionListener(e -> {
            try {
                if (vendaRef[0] == null) throw new IllegalArgumentException("Busque uma venda primeiro.");
                ItemVenda ivDev = (ItemVenda) cmbProdDev.getSelectedItem();
                if (ivDev == null) throw new IllegalArgumentException("Selecione o produto a devolver.");
                Produto pNovo = (Produto) cmbProdNovo.getSelectedItem();
                if (pNovo == null) throw new IllegalArgumentException("Selecione o produto novo.");
                if (txtMotivo.getText().trim().isEmpty()) throw new IllegalArgumentException("O motivo é obrigatório.");

                Troca t = new Troca();
                t.setIdVenda(vendaRef[0].getId());
                t.setIdProdutoDevolvido(ivDev.getIdProduto());
                t.setQuantidadeDevolvida(Integer.parseInt(txtQtdDev.getText().trim()));
                t.setIdProdutoNovo(pNovo.getId());
                t.setQuantidadeNova(Integer.parseInt(txtQtdNova.getText().trim()));
                t.setMotivo(txtMotivo.getText().trim());

                service.realizarTroca(t);

                String msg = "Troca " + t.getNumero() + " registrada!\n";
                if (t.getValorDiferenca().compareTo(BigDecimal.ZERO) > 0)
                    msg += "Cliente deve pagar: " + CURRENCY.format(t.getValorDiferenca());
                else if (t.getValorDiferenca().compareTo(BigDecimal.ZERO) < 0)
                    msg += "Loja deve devolver: " + CURRENCY.format(t.getValorDiferenca().abs());
                else
                    msg += "Sem diferença de valor.";

                JOptionPane.showMessageDialog(dialog, msg);
                dialog.dispose();
                carregar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void verDetalhes() {
        Troca t = getSelecionado();
        if (t == null) return;
        String dif;
        if (t.getValorDiferenca().compareTo(BigDecimal.ZERO) > 0)
            dif = "Cliente pagou: " + CURRENCY.format(t.getValorDiferenca());
        else if (t.getValorDiferenca().compareTo(BigDecimal.ZERO) < 0)
            dif = "Loja devolveu: " + CURRENCY.format(t.getValorDiferenca().abs());
        else dif = "Sem diferença de valor";

        String msg = String.format("""
                Troca: %s
                Venda: %s
                Cliente: %s
                Data: %s
                Motivo: %s

                Produto devolvido: %s (qtd: %d)
                Produto novo: %s (qtd: %d)

                Diferença: %s
                """,
                t.getNumero(), t.getNumeroVenda(), t.getNomeCliente(),
                t.getDataTroca().format(FMT), t.getMotivo(),
                t.getNomeProdutoDevolvido(), t.getQuantidadeDevolvida(),
                t.getNomeProdutoNovo(), t.getQuantidadeNova(), dif);
        JOptionPane.showMessageDialog(this, msg, "Detalhes da Troca", JOptionPane.INFORMATION_MESSAGE);
    }
}
