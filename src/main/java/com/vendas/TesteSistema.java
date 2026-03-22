package com.vendas;

import com.vendas.dao.*;
import com.vendas.model.*;
import com.vendas.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Teste de integração completo: testa todas as transações e regras de negócio.
 */
public class TesteSistema {

    static int pass = 0, fail = 0;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        // Usa banco temporário para testes
        System.setProperty("user.home", System.getProperty("java.io.tmpdir"));
        // Remove banco anterior
        new java.io.File(System.getProperty("java.io.tmpdir") + "/sistema_vendas.db").delete();

        com.vendas.database.DatabaseConnection.getInstance(); // cria tabelas

        System.out.println("=================================================");
        System.out.println("  TESTE DO SISTEMA DE VENDAS");
        System.out.println("=================================================\n");

        testarCRUDCategoria();
        testarCRUDProduto();
        testarCRUDCliente();
        testarOrcamento();
        testarVenda();
        testarDevolucao();
        testarTroca();
        testarRelatorios();

        System.out.println("\n=================================================");
        System.out.printf("  RESULTADO: %d PASSOU  |  %d FALHOU%n", pass, fail);
        System.out.println("=================================================");

        if (fail > 0) System.exit(1);
    }

    // ─── Utilitários ───────────────────────────────────────────────────────────

    static void ok(String descricao) {
        System.out.println("  [OK] " + descricao);
        pass++;
    }

    static void falha(String descricao, String motivo) {
        System.out.println("  [FALHA] " + descricao + " -> " + motivo);
        fail++;
    }

    static void secao(String titulo) {
        System.out.println("\n--- " + titulo + " ---");
    }

    static void assertVerdadeiro(String desc, boolean cond) {
        if (cond) ok(desc); else falha(desc, "condição falsa");
    }

    static void assertExcecao(String desc, Runnable r) {
        try { r.run(); falha(desc, "esperava exceção mas não foi lançada"); }
        catch (Exception e) { ok(desc + " [exceção: " + e.getMessage() + "]"); }
    }

    // ─── Fixtures ──────────────────────────────────────────────────────────────

    static Cliente criarCliente(String nome, String cpf) {
        ClienteDAO dao = new ClienteDAO();
        Cliente c = new Cliente();
        c.setNome(nome); c.setCpf(cpf);
        c.setEmail(nome.toLowerCase().replace(" ","") + "@email.com");
        c.setTelefone("(31) 9999-0000");
        c.setDataCadastro(LocalDate.now());
        dao.save(c);
        return c;
    }

    static Produto criarProduto(String codigo, String nome, double preco, int estoque) {
        ProdutoDAO dao = new ProdutoDAO();
        Produto p = new Produto();
        p.setCodigo(codigo); p.setNome(nome);
        p.setPreco(BigDecimal.valueOf(preco));
        p.setEstoque(estoque); p.setEstoqueMinimo(2);
        dao.save(p);
        return p;
    }

    // ─── Testes ────────────────────────────────────────────────────────────────

    static void testarCRUDCategoria() {
        secao("1. CRUD CATEGORIA");
        CategoriaDAO dao = new CategoriaDAO();

        Categoria cat = new Categoria();
        cat.setNome("Eletrônicos"); cat.setDescricao("Produtos eletrônicos");
        dao.save(cat);
        assertVerdadeiro("Save categoria gera ID", cat.getId() > 0);

        cat.setNome("Eletrodomésticos");
        dao.update(cat);
        Categoria found = dao.findById(cat.getId());
        assertVerdadeiro("Update categoria", "Eletrodomésticos".equals(found.getNome()));

        List<Categoria> all = dao.findAll();
        assertVerdadeiro("FindAll retorna pelo menos 1", all.size() >= 1);

        dao.delete(cat.getId());
        assertVerdadeiro("Delete categoria", dao.findById(cat.getId()) == null);
    }

    static void testarCRUDProduto() {
        secao("2. CRUD PRODUTO");
        ProdutoDAO dao = new ProdutoDAO();

        Produto p = criarProduto("PRD001", "Notebook Dell", 3500.00, 10);
        assertVerdadeiro("Save produto gera ID", p.getId() > 0);

        p.setPreco(BigDecimal.valueOf(3200.00));
        dao.update(p);
        Produto found = dao.findById(p.getId());
        assertVerdadeiro("Update preço do produto", found.getPreco().compareTo(BigDecimal.valueOf(3200.00)) == 0);

        criarProduto("PRD002", "Mouse Logitech", 150.00, 50);
        List<Produto> busca = dao.findByNomeOrCodigo("Mouse");
        assertVerdadeiro("Busca por nome retorna resultado", !busca.isEmpty());

        dao.atualizarEstoque(p.getId(), 8);
        assertVerdadeiro("Atualizar estoque", dao.findById(p.getId()).getEstoque() == 8);
    }

    static void testarCRUDCliente() {
        secao("3. CRUD CLIENTE");
        ClienteDAO dao = new ClienteDAO();

        Cliente c = criarCliente("João Silva", "111.111.111-11");
        assertVerdadeiro("Save cliente gera ID", c.getId() > 0);

        c.setEmail("joao@gmail.com");
        dao.update(c);
        assertVerdadeiro("Update email cliente", "joao@gmail.com".equals(dao.findById(c.getId()).getEmail()));

        List<Cliente> busca = dao.findByNomeOrCpf("João");
        assertVerdadeiro("Busca cliente por nome", !busca.isEmpty());

        criarCliente("Maria Santos", "222.222.222-22");
        assertVerdadeiro("FindAll >= 2 clientes", dao.findAll().size() >= 2);
    }

    static Orcamento testarOrcamento() {
        secao("4. TRANSAÇÃO: REALIZAR ORÇAMENTO");
        OrcamentoService service = new OrcamentoService();
        OrcamentoDAO dao = new OrcamentoDAO();
        ProdutoDAO produtoDAO = new ProdutoDAO();

        Cliente cliente = new ClienteDAO().findByNomeOrCpf("João").get(0);
        Produto produto = produtoDAO.findByNomeOrCodigo("Notebook").get(0);

        // RN01: orçamento sem itens deve falhar
        assertExcecao("RN01 - Orçamento sem itens lança exceção", () -> {
            Orcamento o = new Orcamento();
            o.setIdCliente(cliente.getId());
            service.criarOrcamento(o);
        });

        // RN02: quantidade zero deve falhar
        assertExcecao("RN02 - Quantidade zero lança exceção", () -> {
            Orcamento o = new Orcamento();
            o.setIdCliente(cliente.getId());
            ItemOrcamento item = new ItemOrcamento();
            item.setIdProduto(produto.getId());
            item.setQuantidade(0);
            o.getItens().add(item);
            service.criarOrcamento(o);
        });

        // RN03: estoque insuficiente deve falhar
        assertExcecao("RN03 - Estoque insuficiente lança exceção", () -> {
            Orcamento o = new Orcamento();
            o.setIdCliente(cliente.getId());
            ItemOrcamento item = new ItemOrcamento();
            item.setIdProduto(produto.getId());
            item.setQuantidade(9999); // muito além do estoque
            o.getItens().add(item);
            service.criarOrcamento(o);
        });

        // Criação bem-sucedida
        Orcamento orc = new Orcamento();
        orc.setIdCliente(cliente.getId());
        ItemOrcamento item = new ItemOrcamento();
        item.setIdProduto(produto.getId());
        item.setQuantidade(2);
        orc.getItens().add(item);
        service.criarOrcamento(orc);

        assertVerdadeiro("Orçamento criado com ID", orc.getId() > 0);
        assertVerdadeiro("Número gerado", orc.getNumero() != null && orc.getNumero().startsWith("ORC-"));
        assertVerdadeiro("Status PENDENTE", "PENDENTE".equals(orc.getStatus()));

        // RN04: validade = hoje + 7 dias
        assertVerdadeiro("RN04 - Validade = hoje + 7 dias",
                orc.getDataValidade().equals(LocalDate.now().plusDays(7)));

        // Verifica total calculado
        Produto pCheck = produtoDAO.findById(produto.getId());
        BigDecimal expectedTotal = pCheck.getPreco().multiply(BigDecimal.valueOf(2));
        assertVerdadeiro("Valor total calculado corretamente",
                orc.getValorTotal().compareTo(expectedTotal) == 0);

        // RN05: cancelar orçamento PENDENTE
        service.cancelarOrcamento(orc.getId());
        assertVerdadeiro("RN05 - Cancelar orçamento PENDENTE", "CANCELADO".equals(dao.findById(orc.getId()).getStatus()));

        // RN05: não pode cancelar orçamento já cancelado
        assertExcecao("RN05 - Não cancela orçamento já CANCELADO", () -> service.cancelarOrcamento(orc.getId()));

        // Cria orçamento para conversão em venda
        Orcamento orcParaVenda = new Orcamento();
        orcParaVenda.setIdCliente(cliente.getId());
        ItemOrcamento item2 = new ItemOrcamento();
        item2.setIdProduto(produto.getId());
        item2.setQuantidade(1);
        orcParaVenda.getItens().add(item2);
        service.criarOrcamento(orcParaVenda);
        assertVerdadeiro("Segundo orçamento criado para testes", orcParaVenda.getId() > 0);

        return orcParaVenda;
    }

    static Venda testarVenda() {
        secao("5. TRANSAÇÃO: EFETUAR VENDA");
        VendaService service = new VendaService();
        VendaDAO vendaDAO = new VendaDAO();
        ProdutoDAO produtoDAO = new ProdutoDAO();

        Cliente cliente = new ClienteDAO().findByNomeOrCpf("Maria").get(0);
        Produto mouse = produtoDAO.findByNomeOrCodigo("Mouse").get(0);
        int estoqueAntes = mouse.getEstoque();

        // RN01: venda sem itens
        assertExcecao("RN01 - Venda sem itens lança exceção", () -> {
            Venda v = new Venda();
            v.setIdCliente(cliente.getId());
            v.setFormaPagamento("Dinheiro");
            service.efetuarVenda(v);
        });

        // RN02: estoque insuficiente
        assertExcecao("RN02 - Estoque insuficiente lança exceção", () -> {
            Venda v = new Venda();
            v.setIdCliente(cliente.getId());
            v.setFormaPagamento("PIX");
            ItemVenda iv = new ItemVenda();
            iv.setIdProduto(mouse.getId());
            iv.setQuantidade(9999);
            v.getItens().add(iv);
            service.efetuarVenda(v);
        });

        // RN03: desconto > total
        assertExcecao("RN03 - Desconto maior que total lança exceção", () -> {
            Venda v = new Venda();
            v.setIdCliente(cliente.getId());
            v.setFormaPagamento("Dinheiro");
            v.setDesconto(BigDecimal.valueOf(9999));
            ItemVenda iv = new ItemVenda();
            iv.setIdProduto(mouse.getId());
            iv.setQuantidade(1);
            v.getItens().add(iv);
            service.efetuarVenda(v);
        });

        // Venda bem-sucedida
        Venda venda = new Venda();
        venda.setIdCliente(cliente.getId());
        venda.setFormaPagamento("Cartão de Crédito");
        venda.setDesconto(BigDecimal.valueOf(10));
        ItemVenda iv = new ItemVenda();
        iv.setIdProduto(mouse.getId());
        iv.setQuantidade(3);
        venda.getItens().add(iv);
        service.efetuarVenda(venda);

        assertVerdadeiro("Venda criada com ID", venda.getId() > 0);
        assertVerdadeiro("Status CONCLUIDA", "CONCLUIDA".equals(venda.getStatus()));
        assertVerdadeiro("Número gerado com prefixo VND-", venda.getNumero().startsWith("VND-"));

        // RN05: verifica que estoque foi baixado
        int estoqueDepois = produtoDAO.findById(mouse.getId()).getEstoque();
        assertVerdadeiro("RN05 - Estoque reduzido em 3", estoqueDepois == estoqueAntes - 3);

        // Valor final = total - desconto
        BigDecimal total = mouse.getPreco().multiply(BigDecimal.valueOf(3));
        BigDecimal finalEsperado = total.subtract(BigDecimal.valueOf(10));
        assertVerdadeiro("Valor final = total - desconto", venda.getValorFinal().compareTo(finalEsperado) == 0);

        // Conversão de orçamento em venda
        OrcamentoDAO orcDAO = new OrcamentoDAO();
        Produto notebook = produtoDAO.findByNomeOrCodigo("Notebook").get(0);
        // Pega primeiro orçamento pendente
        List<Orcamento> pendentes = orcDAO.findPendentes();
        assertVerdadeiro("Existe orçamento pendente para converter", !pendentes.isEmpty());

        Venda vendaDeOrc = service.converterOrcamentoEmVenda(
                pendentes.get(0).getId(), "PIX", BigDecimal.ZERO, "Conversão de orçamento");
        assertVerdadeiro("RN04 - Venda criada a partir de orçamento", vendaDeOrc.getId() > 0);
        assertVerdadeiro("Orçamento marcado como APROVADO",
                "APROVADO".equals(orcDAO.findById(pendentes.get(0).getId()).getStatus()));

        // RN04: não pode converter orçamento já aprovado
        assertExcecao("RN04 - Não converte orçamento já APROVADO", () ->
            service.converterOrcamentoEmVenda(pendentes.get(0).getId(), "Dinheiro", BigDecimal.ZERO, ""));

        return venda;
    }

    static void testarDevolucao() {
        secao("6. TRANSAÇÃO: REALIZAR DEVOLUÇÃO");
        DevolucaoService service = new DevolucaoService();
        VendaDAO vendaDAO = new VendaDAO();
        ProdutoDAO produtoDAO = new ProdutoDAO();

        // Pega venda CONCLUIDA
        Venda venda = vendaDAO.findAll().stream()
                .filter(v -> "CONCLUIDA".equals(v.getStatus()))
                .findFirst().orElseThrow(() -> new RuntimeException("Nenhuma venda CONCLUIDA encontrada"));

        venda = vendaDAO.findById(venda.getId()); // carrega itens
        int estoqueAntes = produtoDAO.findById(venda.getItens().get(0).getIdProduto()).getEstoque();

        // RN02: não pode devolver venda já devolvida (precisamos ter uma)
        // Primeiro testa regra de qtd excedida
        final Venda vendaFinal = venda;
        assertExcecao("RN03 - Qtd devolvida > qtd vendida lança exceção", () -> {
            Devolucao dev = new Devolucao();
            dev.setIdVenda(vendaFinal.getId());
            dev.setMotivo("Defeito");
            ItemDevolucao id = new ItemDevolucao();
            id.setIdProduto(vendaFinal.getItens().get(0).getIdProduto());
            id.setQuantidade(vendaFinal.getItens().get(0).getQuantidade() + 100);
            dev.getItens().add(id);
            service.realizarDevolucao(dev);
        });

        // Devolução bem-sucedida
        Devolucao dev = new Devolucao();
        dev.setIdVenda(venda.getId());
        dev.setMotivo("Produto com defeito");
        ItemDevolucao itemDev = new ItemDevolucao();
        itemDev.setIdProduto(venda.getItens().get(0).getIdProduto());
        itemDev.setQuantidade(1);
        dev.getItens().add(itemDev);
        service.realizarDevolucao(dev);

        assertVerdadeiro("Devolução criada com ID", dev.getId() > 0);
        assertVerdadeiro("Número gerado com DEV-", dev.getNumero().startsWith("DEV-"));
        assertVerdadeiro("Valor devolvido calculado", dev.getValorDevolvido().compareTo(BigDecimal.ZERO) > 0);

        // RN04: estoque foi reabastecido
        int estoqueDepois = produtoDAO.findById(venda.getItens().get(0).getIdProduto()).getEstoque();
        assertVerdadeiro("RN04 - Estoque reabastecido em 1", estoqueDepois == estoqueAntes + 1);

        // Venda deve estar DEVOLVIDA
        assertVerdadeiro("Venda marcada como DEVOLVIDA",
                "DEVOLVIDA".equals(vendaDAO.findById(venda.getId()).getStatus()));

        // RN02: não pode devolver venda DEVOLVIDA
        assertExcecao("RN02 - Não devolve venda já DEVOLVIDA", () -> {
            Devolucao d2 = new Devolucao();
            d2.setIdVenda(vendaFinal.getId());
            d2.setMotivo("teste");
            ItemDevolucao id2 = new ItemDevolucao();
            id2.setIdProduto(vendaFinal.getItens().get(0).getIdProduto());
            id2.setQuantidade(1);
            d2.getItens().add(id2);
            service.realizarDevolucao(d2);
        });
    }

    static void testarTroca() {
        secao("7. TRANSAÇÃO: REALIZAR TROCA");
        TrocaService service = new TrocaService();
        VendaDAO vendaDAO = new VendaDAO();
        ProdutoDAO produtoDAO = new ProdutoDAO();

        // Pega uma venda CONCLUIDA
        Venda venda = vendaDAO.findAll().stream()
                .filter(v -> "CONCLUIDA".equals(v.getStatus()))
                .findFirst().orElseThrow();
        venda = vendaDAO.findById(venda.getId());

        Produto prodNovo = produtoDAO.findByNomeOrCodigo("Notebook").get(0);
        int estProdDev  = produtoDAO.findById(venda.getItens().get(0).getIdProduto()).getEstoque();
        int estProdNovo = prodNovo.getEstoque();

        // RN02: produto não está na venda
        final Venda vendaFinal = venda;
        assertExcecao("RN02 - Produto não encontrado na venda lança exceção", () -> {
            Troca t = new Troca();
            t.setIdVenda(vendaFinal.getId());
            t.setIdProdutoDevolvido(99999); // ID inexistente
            t.setQuantidadeDevolvida(1);
            t.setIdProdutoNovo(prodNovo.getId());
            t.setQuantidadeNova(1);
            t.setMotivo("Teste");
            service.realizarTroca(t);
        });

        // RN03: qtd devolvida > qtd vendida
        assertExcecao("RN03 - Qtd troca > qtd vendida lança exceção", () -> {
            Troca t = new Troca();
            t.setIdVenda(vendaFinal.getId());
            t.setIdProdutoDevolvido(vendaFinal.getItens().get(0).getIdProduto());
            t.setQuantidadeDevolvida(vendaFinal.getItens().get(0).getQuantidade() + 100);
            t.setIdProdutoNovo(prodNovo.getId());
            t.setQuantidadeNova(1);
            t.setMotivo("Teste");
            service.realizarTroca(t);
        });

        // Troca bem-sucedida
        Troca troca = new Troca();
        troca.setIdVenda(venda.getId());
        troca.setIdProdutoDevolvido(venda.getItens().get(0).getIdProduto());
        troca.setQuantidadeDevolvida(1);
        troca.setIdProdutoNovo(prodNovo.getId());
        troca.setQuantidadeNova(1);
        troca.setMotivo("Produto errado");
        service.realizarTroca(troca);

        assertVerdadeiro("Troca criada com ID", troca.getId() > 0);
        assertVerdadeiro("Número gerado com TRC-", troca.getNumero().startsWith("TRC-"));

        // RN05: diferença de valor calculada corretamente
        BigDecimal precoDevolvido = venda.getItens().get(0).getPrecoUnitario();
        BigDecimal precoNovo      = prodNovo.getPreco();
        BigDecimal difEsperada    = precoNovo.subtract(precoDevolvido);
        assertVerdadeiro("RN05 - Diferença de valor calculada",
                troca.getValorDiferenca().compareTo(difEsperada) == 0);

        // Estoques atualizados
        int estDevDepois  = produtoDAO.findById(venda.getItens().get(0).getIdProduto()).getEstoque();
        int estNovoDepois = produtoDAO.findById(prodNovo.getId()).getEstoque();
        assertVerdadeiro("Estoque do produto devolvido reabastecido", estDevDepois == estProdDev + 1);
        assertVerdadeiro("Estoque do produto novo reduzido", estNovoDepois == estProdNovo - 1);
    }

    static void testarRelatorios() {
        secao("8. RELATÓRIOS / CONSULTAS");
        VendaDAO vendaDAO = new VendaDAO();
        OrcamentoDAO orcDAO = new OrcamentoDAO();
        DevolucaoDAO devDAO = new DevolucaoDAO();

        // Relatório 1: Vendas por período
        List<Venda> vendasHoje = vendaDAO.findByPeriodo(LocalDate.now(), LocalDate.now());
        assertVerdadeiro("Relatório 1 - Vendas por período retorna resultados", !vendasHoje.isEmpty());

        // Relatório 2: Produtos mais vendidos
        List<Object[]> maisVendidos = vendaDAO.relatorioMaisVendidos();
        assertVerdadeiro("Relatório 2 - Produtos mais vendidos não vazio", !maisVendidos.isEmpty());
        System.out.println("     Top produto: " + maisVendidos.get(0)[0] + " (" + maisVendidos.get(0)[1] + " unidades)");

        // Relatório 3: Melhores clientes
        List<Object[]> clientes = vendaDAO.relatorioMelhoresClientes();
        assertVerdadeiro("Relatório 3 - Melhores clientes não vazio", !clientes.isEmpty());
        System.out.println("     Top cliente: " + clientes.get(0)[0] + " (" + clientes.get(0)[2] + ")");

        // Relatório 4: Orçamentos pendentes
        orcDAO.atualizarOrcamentosVencidos();
        List<Orcamento> pendentes = orcDAO.findPendentes();
        assertVerdadeiro("Relatório 4 - Orçamentos pendentes consultável", pendentes != null);
        System.out.println("     Orçamentos pendentes: " + pendentes.size());

        // Relatório 5: Devoluções
        List<Devolucao> devs = devDAO.findAll();
        assertVerdadeiro("Relatório 5 - Devoluções consultável", !devs.isEmpty());
        System.out.println("     Total de devoluções: " + devs.size());
    }
}
