package com.vendas.ui;

import com.vendas.dao.ClienteDAO;
import com.vendas.model.Cliente;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientePanel extends JPanel {

    private final ClienteDAO dao = new ClienteDAO();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField txtBusca = new JTextField(20);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ClientePanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Barra de busca
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Buscar:"));
        topPanel.add(txtBusca);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnNovo   = new JButton("Novo Cliente");
        JButton btnEditar = new JButton("Editar");
        JButton btnExcluir = new JButton("Excluir");
        topPanel.add(btnBuscar);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(btnNovo);
        topPanel.add(btnEditar);
        topPanel.add(btnExcluir);
        add(topPanel, BorderLayout.NORTH);

        // Tabela
        tableModel = new DefaultTableModel(new String[]{"ID", "Nome", "CPF", "Email", "Telefone", "Cadastro"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Ações
        btnBuscar.addActionListener(e -> carregar(txtBusca.getText().trim()));
        txtBusca.addActionListener(e -> carregar(txtBusca.getText().trim()));
        btnNovo.addActionListener(e -> abrirFormulario(null));
        btnEditar.addActionListener(e -> {
            Cliente c = getSelecionado();
            if (c != null) abrirFormulario(c);
        });
        btnExcluir.addActionListener(e -> excluir());

        carregar("");
    }

    private void carregar(String termo) {
        tableModel.setRowCount(0);
        List<Cliente> lista = termo.isEmpty() ? dao.findAll() : dao.findByNomeOrCpf(termo);
        for (Cliente c : lista) {
            tableModel.addRow(new Object[]{
                c.getId(), c.getNome(), c.getCpf(), c.getEmail(), c.getTelefone(),
                c.getDataCadastro() != null ? c.getDataCadastro().format(FMT) : ""
            });
        }
    }

    private Cliente getSelecionado() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecione um cliente."); return null; }
        int id = (int) tableModel.getValueAt(row, 0);
        return dao.findById(id);
    }

    private void abrirFormulario(Cliente cliente) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                cliente == null ? "Novo Cliente" : "Editar Cliente", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField txtNome     = new JTextField(25);
        JTextField txtCpf      = new JTextField(15);
        JTextField txtEmail    = new JTextField(25);
        JTextField txtTelefone = new JTextField(15);
        JTextField txtEndereco = new JTextField(30);

        if (cliente != null) {
            txtNome.setText(cliente.getNome());
            txtCpf.setText(cliente.getCpf());
            txtEmail.setText(cliente.getEmail());
            txtTelefone.setText(cliente.getTelefone());
            txtEndereco.setText(cliente.getEndereco());
        }

        String[][] fields = {{"Nome*:", ""}, {"CPF*:", ""}, {"Email:", ""}, {"Telefone:", ""}, {"Endereço:", ""}};
        JTextField[] inputs = {txtNome, txtCpf, txtEmail, txtTelefone, txtEndereco};

        for (int i = 0; i < inputs.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0;
            form.add(new JLabel(fields[i][0]), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            form.add(inputs[i], gbc);
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSalvar = new JButton("Salvar");
        JButton btnCancel = new JButton("Cancelar");
        btnPanel.add(btnSalvar);
        btnPanel.add(btnCancel);

        dialog.setLayout(new BorderLayout());
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSalvar.addActionListener(e -> {
            String nome = txtNome.getText().trim();
            String cpf  = txtCpf.getText().trim();
            if (nome.isEmpty() || cpf.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Nome e CPF são obrigatórios.");
                return;
            }
            try {
                Cliente c = cliente != null ? cliente : new Cliente();
                c.setNome(nome);
                c.setCpf(cpf);
                c.setEmail(txtEmail.getText().trim());
                c.setTelefone(txtTelefone.getText().trim());
                c.setEndereco(txtEndereco.getText().trim());
                if (c.getDataCadastro() == null) c.setDataCadastro(LocalDate.now());

                if (cliente == null) dao.save(c);
                else dao.update(c);

                dialog.dispose();
                carregar(txtBusca.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    private void excluir() {
        Cliente c = getSelecionado();
        if (c == null) return;
        int opt = JOptionPane.showConfirmDialog(this,
                "Excluir cliente: " + c.getNome() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                dao.delete(c.getId());
                carregar(txtBusca.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
