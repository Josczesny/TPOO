package com.vendas.ui;

import com.vendas.dao.DevolucaoDAO;
import com.vendas.dao.OrcamentoDAO;
import com.vendas.dao.VendaDAO;
import com.vendas.model.Devolucao;
import com.vendas.model.Orcamento;
import com.vendas.model.Venda;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Relatórios disponíveis:
 * 1. Vendas por Período
 * 2. Produtos Mais Vendidos
 * 3. Melhores Clientes
 * 4. Orçamentos Pendentes
 * 5. Relatório de Devoluções
 */
public class RelatorioPanel extends JPanel {

    private final VendaDAO vendaDAO = new VendaDAO();
    private final OrcamentoDAO orcDAO = new OrcamentoDAO();
    private final DevolucaoDAO devDAO = new DevolucaoDAO();
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_D  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTabbedPane tabs = new JTabbedPane();

    public RelatorioPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tabs.addTab("1. Vendas por Período",    criarVendasPorPeriodo());
        tabs.addTab("2. Produtos Mais Vendidos", criarProdutosMaisVendidos());
        tabs.addTab("3. Melhores Clientes",      criarMelhoresClientes());
        tabs.addTab("4. Orçamentos Pendentes",   criarOrcamentosPendentes());
        tabs.addTab("5. Devoluções",             criarRelDevoluções());

        add(tabs, BorderLayout.CENTER);
    }

    // ── Relatório 1: Vendas por Período ──────────────────────────────────────
    private JPanel criarVendasPorPeriodo() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtInicio = new JTextField(LocalDate.now().withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE), 12);
        JTextField txtFim    = new JTextField(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), 12);
        JButton btnGerar     = new JButton("Gerar Relatório");
        filtro.add(new JLabel("De (AAAA-MM-DD):"));
        filtro.add(txtInicio);
        filtro.add(new JLabel("Até:"));
        filtro.add(txtFim);
        filtro.add(btnGerar);
        panel.add(filtro, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Número", "Cliente", "Data/Hora", "Pagamento", "Total", "Desconto", "Final", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        JLabel lblResumo = new JLabel("  Gere o relatório para ver os resultados.");

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(lblResumo, BorderLayout.SOUTH);

        btnGerar.addActionListener(e -> {
            try {
                LocalDate ini = LocalDate.parse(txtInicio.getText().trim());
                LocalDate fim = LocalDate.parse(txtFim.getText().trim());
                if (ini.isAfter(fim)) throw new IllegalArgumentException("Data inicial deve ser <= data final.");

                model.setRowCount(0);
                List<Venda> vendas = vendaDAO.findByPeriodo(ini, fim);
                double totalGeral = 0;
                for (Venda v : vendas) {
                    model.addRow(new Object[]{
                        v.getNumero(), v.getNomeCliente(), v.getDataVenda().format(FMT_DT),
                        v.getFormaPagamento(), CURRENCY.format(v.getValorTotal()),
                        CURRENCY.format(v.getDesconto()), CURRENCY.format(v.getValorFinal()),
                        v.getStatus()
                    });
                    totalGeral += v.getValorFinal().doubleValue();
                }
                lblResumo.setText(String.format("  %d vendas encontradas | Total do período: %s",
                        vendas.size(), CURRENCY.format(totalGeral)));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    // ── Relatório 2: Produtos Mais Vendidos ──────────────────────────────────
    private JPanel criarProdutosMaisVendidos() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"#", "Produto", "Qtd Vendida", "Receita Total"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);

        JButton btnGerar = new JButton("Gerar");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Top 10 Produtos Mais Vendidos"));
        top.add(btnGerar);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        btnGerar.addActionListener(e -> {
            model.setRowCount(0);
            List<Object[]> rows = vendaDAO.relatorioMaisVendidos();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r = rows.get(i);
                model.addRow(new Object[]{i + 1, r[0], r[1], CURRENCY.format(r[2])});
            }
        });

        btnGerar.doClick();
        return panel;
    }

    // ── Relatório 3: Melhores Clientes ───────────────────────────────────────
    private JPanel criarMelhoresClientes() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"#", "Cliente", "Qtd Compras", "Total Gasto"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);

        JButton btnGerar = new JButton("Gerar");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Top 10 Clientes com Mais Compras"));
        top.add(btnGerar);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        btnGerar.addActionListener(e -> {
            model.setRowCount(0);
            List<Object[]> rows = vendaDAO.relatorioMelhoresClientes();
            for (int i = 0; i < rows.size(); i++) {
                Object[] r = rows.get(i);
                model.addRow(new Object[]{i + 1, r[0], r[1], CURRENCY.format(r[2])});
            }
        });

        btnGerar.doClick();
        return panel;
    }

    // ── Relatório 4: Orçamentos Pendentes ────────────────────────────────────
    private JPanel criarOrcamentosPendentes() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Número", "Cliente", "Emissão", "Validade", "Valor Total", "Dias Restantes"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        // Destaca linhas com vencimento próximo
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    Object dias = model.getValueAt(row, 5);
                    if (dias instanceof Long d) {
                        setBackground(d <= 1 ? new Color(255, 200, 200) : d <= 3 ? new Color(255, 240, 180) : Color.WHITE);
                    } else setBackground(Color.WHITE);
                }
                return this;
            }
        });

        JButton btnGerar = new JButton("Atualizar");
        JLabel lblLegenda = new JLabel("  Vermelho = vence hoje/amanhã | Amarelo = vence em ≤ 3 dias");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Orçamentos Pendentes:")); top.add(btnGerar); top.add(lblLegenda);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        btnGerar.addActionListener(e -> {
            model.setRowCount(0);
            orcDAO.atualizarOrcamentosVencidos();
            List<Orcamento> lista = orcDAO.findPendentes();
            for (Orcamento o : lista) {
                long diasRestantes = LocalDate.now().until(o.getDataValidade(), java.time.temporal.ChronoUnit.DAYS);
                model.addRow(new Object[]{
                    o.getNumero(), o.getNomeCliente(),
                    o.getDataEmissao().format(FMT_D), o.getDataValidade().format(FMT_D),
                    CURRENCY.format(o.getValorTotal()), diasRestantes
                });
            }
        });

        btnGerar.doClick();
        return panel;
    }

    // ── Relatório 5: Devoluções ───────────────────────────────────────────────
    private JPanel criarRelDevoluções() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Número", "Nº Venda", "Cliente", "Data/Hora", "Motivo", "Valor Devolvido"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);

        JButton btnGerar = new JButton("Atualizar");
        JLabel lblTotal  = new JLabel("  Total devolvido: ---");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Histórico de Devoluções:")); top.add(btnGerar); top.add(lblTotal);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        btnGerar.addActionListener(e -> {
            model.setRowCount(0);
            List<Devolucao> lista = devDAO.findAll();
            double total = 0;
            for (Devolucao d : lista) {
                model.addRow(new Object[]{
                    d.getNumero(), d.getNumeroVenda(), d.getNomeCliente(),
                    d.getDataDevolucao().format(FMT_DT), d.getMotivo(),
                    CURRENCY.format(d.getValorDevolvido())
                });
                total += d.getValorDevolvido().doubleValue();
            }
            lblTotal.setText("  " + lista.size() + " devoluções | Total devolvido: " + CURRENCY.format(total));
        });

        btnGerar.doClick();
        return panel;
    }
}
