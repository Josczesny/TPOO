package com.vendas.ui;

import com.vendas.dao.CategoriaDAO;
import com.vendas.dao.ProdutoDAO;
import com.vendas.model.Categoria;
import com.vendas.model.Produto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProdutoPanel extends JPanel {

    private final ProdutoDAO dao = new ProdutoDAO();
    private final CategoriaDAO catDAO = new CategoriaDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField txtBusca = new JTextField(20);
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public ProdutoPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Buscar:"));
        topPanel.add(txtBusca);
        JButton btnBuscar  = new JButton("Buscar");
        JButton btnNovo    = new JButton("Novo Produto");
        JButton btnEditar  = new JButton("Editar");
        JButton btnExcluir = new JButton("Excluir");
        JButton btnCat     = new JButton("Categorias");
        topPanel.add(btnBuscar);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(btnNovo);
        topPanel.add(btnEditar);
        topPanel.add(btnExcluir);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(btnCat);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Código", "Nome", "Preço", "Estoque", "Est.Mínimo", "Categoria"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnBuscar.addActionListener(e -> carregar(txtBusca.getText().trim()));
        txtBusca.addActionListener(e -> carregar(txtBusca.getText().trim()));
        btnNovo.addActionListener(e -> abrirFormulario(null));
        btnEditar.addActionListener(e -> { Produto p = getSelecionado(); if (p != null) abrirFormulario(p); });
        btnExcluir.addActionListener(e -> excluir());
        btnCat.addActionListener(e -> gerenciarCategorias());

        carregar("");
    }

    private void carregar(String termo) {
        tableModel.setRowCount(0);
        List<Produto> lista = termo.isEmpty() ? dao.findAll() : dao.findByNomeOrCodigo(termo);
        for (Produto p : lista) {
            tableModel.addRow(new Object[]{
                p.getId(), p.getCodigo(), p.getNome(),
                CURRENCY.format(p.getPreco()), p.getEstoque(), p.getEstoqueMinimo(),
                p.getNomeCategoria() != null ? p.getNomeCategoria() : ""
            });
        }
    }

    private Produto getSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione um produto."); return null; }
        return dao.findById((int) tableModel.getValueAt(row, 0));
    }

    private void abrirFormulario(Produto produto) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                produto == null ? "Novo Produto" : "Editar Produto", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(480, 400);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField txtCodigo    = new JTextField(15);
        JTextField txtNome      = new JTextField(25);
        JTextField txtDescricao = new JTextField(30);
        JTextField txtPreco     = new JTextField(10);
        JTextField txtEstoque   = new JTextField(8);
        JTextField txtEstMin    = new JTextField(8);
        JComboBox<Categoria> cmbCat = new JComboBox<>();
        cmbCat.addItem(new Categoria(0, "Sem categoria", ""));
        catDAO.findAll().forEach(cmbCat::addItem);

        if (produto != null) {
            txtCodigo.setText(produto.getCodigo());
            txtNome.setText(produto.getNome());
            txtDescricao.setText(produto.getDescricao());
            txtPreco.setText(produto.getPreco().toPlainString());
            txtEstoque.setText(String.valueOf(produto.getEstoque()));
            txtEstMin.setText(String.valueOf(produto.getEstoqueMinimo()));
            for (int i = 0; i < cmbCat.getItemCount(); i++) {
                if (cmbCat.getItemAt(i).getId() == produto.getIdCategoria()) {
                    cmbCat.setSelectedIndex(i); break;
                }
            }
        }

        Object[][] rows = {
            {"Código*:", txtCodigo}, {"Nome*:", txtNome}, {"Descrição:", txtDescricao},
            {"Preço*:", txtPreco}, {"Estoque*:", txtEstoque}, {"Estoque Mínimo:", txtEstMin},
            {"Categoria:", cmbCat}
        };

        for (int i = 0; i < rows.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            form.add(new JLabel((String) rows[i][0]), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            form.add((Component) rows[i][1], gbc);
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvar = new JButton("Salvar");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSalvar); btnPanel.add(btnCancel);
        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSalvar.addActionListener(e -> {
            try {
                if (txtCodigo.getText().trim().isEmpty() || txtNome.getText().trim().isEmpty())
                    throw new IllegalArgumentException("Código e Nome são obrigatórios.");
                BigDecimal preco = new BigDecimal(txtPreco.getText().trim().replace(",", "."));
                if (preco.compareTo(BigDecimal.ZERO) <= 0)
                    throw new IllegalArgumentException("Preço deve ser maior que zero.");
                int estoque = Integer.parseInt(txtEstoque.getText().trim());
                int estMin  = Integer.parseInt(txtEstMin.getText().trim());

                Produto p = produto != null ? produto : new Produto();
                p.setCodigo(txtCodigo.getText().trim());
                p.setNome(txtNome.getText().trim());
                p.setDescricao(txtDescricao.getText().trim());
                p.setPreco(preco);
                p.setEstoque(estoque);
                p.setEstoqueMinimo(estMin);
                Categoria cat = (Categoria) cmbCat.getSelectedItem();
                p.setIdCategoria(cat != null ? cat.getId() : 0);

                if (produto == null) dao.save(p); else dao.update(p);
                dialog.dispose();
                carregar(txtBusca.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Preço e estoque devem ser números válidos.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void excluir() {
        Produto p = getSelecionado();
        if (p == null) return;
        int opt = JOptionPane.showConfirmDialog(this, "Excluir produto: " + p.getNome() + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            try { dao.delete(p.getId()); carregar(txtBusca.getText().trim()); }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void gerenciarCategorias() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Categorias", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);

        DefaultTableModel catModel = new DefaultTableModel(
                new String[]{"ID", "Nome", "Descrição"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable catTable = new JTable(catModel);
        Runnable reload = () -> {
            catModel.setRowCount(0);
            catDAO.findAll().forEach(c -> catModel.addRow(new Object[]{c.getId(), c.getNome(), c.getDescricao()}));
        };
        reload.run();

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd   = new JButton("Adicionar");
        JButton btnDel   = new JButton("Excluir");
        JButton btnClose = new JButton("Fechar");
        btnPanel.add(btnAdd); btnPanel.add(btnDel); btnPanel.add(btnClose);

        dialog.setLayout(new BorderLayout());
        dialog.add(new JScrollPane(catTable), BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            String nome = JOptionPane.showInputDialog(dialog, "Nome da categoria:");
            if (nome != null && !nome.trim().isEmpty()) {
                Categoria cat = new Categoria();
                cat.setNome(nome.trim());
                catDAO.save(cat);
                reload.run();
            }
        });
        btnDel.addActionListener(e -> {
            int row = catTable.getSelectedRow();
            if (row >= 0) {
                try { catDAO.delete((int) catModel.getValueAt(row, 0)); reload.run(); }
                catch (Exception ex) { JOptionPane.showMessageDialog(dialog, "Erro: " + ex.getMessage()); }
            }
        });
        btnClose.addActionListener(e -> { dialog.dispose(); carregar(txtBusca.getText().trim()); });
        dialog.setVisible(true);
    }
}
