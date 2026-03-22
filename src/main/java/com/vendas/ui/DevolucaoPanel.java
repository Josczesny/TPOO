package com.vendas.ui;

import com.vendas.dao.DevolucaoDAO;
import com.vendas.dao.VendaDAO;
import com.vendas.model.*;
import com.vendas.service.DevolucaoService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DevolucaoPanel extends JPanel {

    private final DevolucaoDAO dao = new DevolucaoDAO();
    private final DevolucaoService service = new DevolucaoService();
    private final VendaDAO vendaDAO = new VendaDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public DevolucaoPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNova      = new JButton("Nova Devolução");
        JButton btnDetalhes  = new JButton("Ver Detalhes");
        JButton btnAtualizar = new JButton("Atualizar");
        topPanel.add(btnNova); topPanel.add(btnDetalhes); topPanel.add(btnAtualizar);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Número", "Nº Venda", "Cliente", "Data/Hora", "Motivo", "Valor Devolvido"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnNova.addActionListener(e -> abrirNovaDevolucao());
        btnDetalhes.addActionListener(e -> verDetalhes());
        btnAtualizar.addActionListener(e -> carregar());

        carregar();
    }

    private void carregar() {
        tableModel.setRowCount(0);
        dao.findAll().forEach(d -> tableModel.addRow(new Object[]{
            d.getId(), d.getNumero(), d.getNumeroVenda(), d.getNomeCliente(),
            d.getDataDevolucao().format(FMT), d.getMotivo(),
            CURRENCY.format(d.getValorDevolvido())
        }));
    }

    private Devolucao getSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione uma devolução."); return null; }
        return dao.findById((int) tableModel.getValueAt(row, 0));
    }

    private void abrirNovaDevolucao() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Nova Devolução", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(750, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(5, 5));

        // Busca por número da venda
        JPanel buscaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtNumVenda = new JTextField(15);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnSelecionar = new JButton("Selecionar Venda...");
        JLabel lblVendaInfo = new JLabel("  Selecione uma venda ou digite o número e clique em Buscar");
        lblVendaInfo.setForeground(Color.GRAY);
        buscaPanel.add(new JLabel("Nº Venda:"));
        buscaPanel.add(txtNumVenda);
        buscaPanel.add(btnBuscar);
        buscaPanel.add(btnSelecionar);
        buscaPanel.add(lblVendaInfo);

        // Tabela de itens da venda (para selecionar o que devolver)
        DefaultTableModel vendaItensModel = new DefaultTableModel(
                new String[]{"ID Produto", "Produto", "Qtd Vendida", "Preço Unit.", "Qtd a Devolver"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        JTable vendaItensTable = new JTable(vendaItensModel);
        vendaItensTable.getColumnModel().getColumn(0).setMaxWidth(0);
        vendaItensTable.getColumnModel().getColumn(0).setMinWidth(0);

        JTextField txtMotivo = new JTextField(40);

        JPanel motivoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        motivoPanel.add(new JLabel("Motivo da Devolução*:"));
        motivoPanel.add(txtMotivo);

        final Venda[] vendaSelecionada = {null};

        // Carrega a venda na tela
        Runnable carregarVenda = () -> {
            String num = txtNumVenda.getText().trim();
            if (num.isEmpty()) return;
            Venda v = vendaDAO.findByNumero(num);
            if (v == null) {
                lblVendaInfo.setText("  Venda não encontrada: " + num);
                lblVendaInfo.setForeground(Color.RED);
                vendaItensModel.setRowCount(0);
                vendaSelecionada[0] = null;
                return;
            }
            vendaSelecionada[0] = v;
            lblVendaInfo.setText("  Cliente: " + v.getNomeCliente() + " | " + v.getDataVenda().format(FMT) + " | Status: " + v.getStatus());
            lblVendaInfo.setForeground(new Color(0, 128, 0));
            vendaItensModel.setRowCount(0);
            v.getItens().forEach(item -> vendaItensModel.addRow(new Object[]{
                item.getIdProduto(), item.getNomeProduto(), item.getQuantidade(),
                CURRENCY.format(item.getPrecoUnitario()), 0
            }));
        };

        btnBuscar.addActionListener(e -> {
            if (txtNumVenda.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Digite o número da venda.");
                return;
            }
            carregarVenda.run();
        });

        // Botão de seleção visual de vendas
        btnSelecionar.addActionListener(e -> {
            List<Venda> vendas = vendaDAO.findAll().stream()
                    .filter(v -> "CONCLUIDA".equals(v.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
            if (vendas.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Nenhuma venda com status CONCLUIDA disponível para devolução.");
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
                int row = listaTable.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(selDialog, "Selecione uma venda."); return; }
                txtNumVenda.setText((String) listaModel.getValueAt(row, 0));
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
        JButton btnSalvar = new JButton("Registrar Devolução");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSalvar); btnPanel.add(btnCancel);

        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.add(new JScrollPane(vendaItensTable), BorderLayout.CENTER);
        center.add(motivoPanel, BorderLayout.SOUTH);

        dialog.add(buscaPanel, BorderLayout.NORTH);
        dialog.add(center, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSalvar.addActionListener(e -> {
            try {
                if (vendaSelecionada[0] == null) throw new IllegalArgumentException("Busque e selecione uma venda primeiro.");
                if (txtMotivo.getText().trim().isEmpty()) throw new IllegalArgumentException("O motivo da devolução é obrigatório.");

                List<ItemDevolucao> itensDev = new ArrayList<>();
                for (int i = 0; i < vendaItensModel.getRowCount(); i++) {
                    Object qtdObj = vendaItensModel.getValueAt(i, 4);
                    int qtd = qtdObj == null ? 0 : Integer.parseInt(qtdObj.toString());
                    if (qtd > 0) {
                        ItemDevolucao id = new ItemDevolucao();
                        id.setIdProduto((int) vendaItensModel.getValueAt(i, 0));
                        id.setQuantidade(qtd);
                        itensDev.add(id);
                    }
                }
                if (itensDev.isEmpty()) throw new IllegalArgumentException("Informe a quantidade a devolver de pelo menos 1 item (coluna 'Qtd a Devolver').");

                Devolucao dev = new Devolucao();
                dev.setIdVenda(vendaSelecionada[0].getId());
                dev.setMotivo(txtMotivo.getText().trim());
                dev.setItens(itensDev);

                service.realizarDevolucao(dev);
                JOptionPane.showMessageDialog(dialog,
                        "Devolução " + dev.getNumero() + " registrada!\nValor a devolver: " + CURRENCY.format(dev.getValorDevolvido()));
                dialog.dispose();
                carregar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void verDetalhes() {
        Devolucao d = getSelecionado();
        if (d == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Devolução: ").append(d.getNumero()).append("\n");
        sb.append("Venda: ").append(d.getNumeroVenda()).append("\n");
        sb.append("Cliente: ").append(d.getNomeCliente()).append("\n");
        sb.append("Data: ").append(d.getDataDevolucao().format(FMT)).append("\n");
        sb.append("Motivo: ").append(d.getMotivo()).append("\n\n");
        sb.append("ITENS DEVOLVIDOS:\n");
        for (ItemDevolucao item : d.getItens()) {
            sb.append(String.format("  - %s: %d x %s = %s%n",
                    item.getNomeProduto(), item.getQuantidade(),
                    CURRENCY.format(item.getPrecoUnitario()), CURRENCY.format(item.getSubtotal())));
        }
        sb.append("\nVALOR DEVOLVIDO: ").append(CURRENCY.format(d.getValorDevolvido()));
        JOptionPane.showMessageDialog(this, sb.toString(), "Detalhes da Devolução", JOptionPane.INFORMATION_MESSAGE);
    }
}
