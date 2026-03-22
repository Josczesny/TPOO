package com.vendas.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Sistema de Vendas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tabs.addTab("Clientes",   new ImageIcon(), new ClientePanel(),   "Cadastro de Clientes");
        tabs.addTab("Produtos",   new ImageIcon(), new ProdutoPanel(),   "Cadastro de Produtos");
        tabs.addTab("Orçamentos", new ImageIcon(), new OrcamentoPanel(), "Realizar Orçamento");
        tabs.addTab("Vendas",     new ImageIcon(), new VendaPanel(),     "Efetuar Venda");
        tabs.addTab("Devoluções", new ImageIcon(), new DevolucaoPanel(), "Realizar Devolução");
        tabs.addTab("Trocas",     new ImageIcon(), new TrocaPanel(),     "Realizar Troca");
        tabs.addTab("Relatórios", new ImageIcon(), new RelatorioPanel(), "Consultas e Relatórios");

        add(tabs, BorderLayout.CENTER);

        JLabel statusBar = new JLabel("  Sistema de Vendas - Pronto");
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        add(statusBar, BorderLayout.SOUTH);
    }
}
