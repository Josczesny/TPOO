package com.vendas.dao;

import com.vendas.database.DatabaseConnection;
import com.vendas.model.ItemVenda;
import com.vendas.model.Venda;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VendaDAO {

    private Connection conn() { return DatabaseConnection.getInstance().getConnection(); }

    private static final String SELECT_BASE =
        "SELECT v.*, c.nome as nome_cliente FROM venda v " +
        "JOIN cliente c ON v.id_cliente = c.id ";

    public List<Venda> findAll() {
        List<Venda> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "ORDER BY v.data_venda DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapHeader(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public Venda findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "WHERE v.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Venda v = mapHeader(rs);
                    v.setItens(findItens(id));
                    return v;
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public Venda findByNumero(String numero) {
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "WHERE v.numero=?")) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Venda v = mapHeader(rs);
                    v.setItens(findItens(v.getId()));
                    return v;
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public List<Venda> findByPeriodo(LocalDate inicio, LocalDate fim) {
        List<Venda> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE date(v.data_venda) BETWEEN ? AND ? ORDER BY v.data_venda DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, inicio.toString());
            ps.setString(2, fim.toString());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapHeader(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public List<ItemVenda> findItens(int idVenda) {
        List<ItemVenda> list = new ArrayList<>();
        String sql = "SELECT iv.*, p.nome as nome_produto FROM item_venda iv " +
                     "JOIN produto p ON iv.id_produto = p.id WHERE iv.id_venda=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idVenda);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemVenda item = new ItemVenda();
                    item.setId(rs.getInt("id"));
                    item.setIdVenda(rs.getInt("id_venda"));
                    item.setIdProduto(rs.getInt("id_produto"));
                    item.setNomeProduto(rs.getString("nome_produto"));
                    item.setQuantidade(rs.getInt("quantidade"));
                    item.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    list.add(item);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public void save(Venda v) {
        String sql = "INSERT INTO venda(numero,id_cliente,id_orcamento,data_venda,forma_pagamento,valor_total,desconto,valor_final,status,observacao) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, v.getNumero());
            ps.setInt(2, v.getIdCliente());
            if (v.getIdOrcamento() != null) ps.setInt(3, v.getIdOrcamento()); else ps.setNull(3, Types.INTEGER);
            ps.setString(4, v.getDataVenda().toString());
            ps.setString(5, v.getFormaPagamento());
            ps.setBigDecimal(6, v.getValorTotal());
            ps.setBigDecimal(7, v.getDesconto());
            ps.setBigDecimal(8, v.getValorFinal());
            ps.setString(9, v.getStatus());
            ps.setString(10, v.getObservacao());
            ps.executeUpdate();
            v.setId(lastId());
            for (ItemVenda item : v.getItens()) {
                item.setIdVenda(v.getId());
                saveItem(item);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private int lastId() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void saveItem(ItemVenda item) throws SQLException {
        String sql = "INSERT INTO item_venda(id_venda,id_produto,quantidade,preco_unitario,subtotal) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, item.getIdVenda());
            ps.setInt(2, item.getIdProduto());
            ps.setInt(3, item.getQuantidade());
            ps.setBigDecimal(4, item.getPrecoUnitario());
            ps.setBigDecimal(5, item.getSubtotal());
            ps.executeUpdate();
            item.setId(lastId());
        }
    }

    public void updateStatus(int id, String status) {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE venda SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // Relatório: produtos mais vendidos
    public List<Object[]> relatorioMaisVendidos() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT p.nome, SUM(iv.quantidade) as total_qty, SUM(iv.subtotal) as total_valor " +
                     "FROM item_venda iv JOIN produto p ON iv.id_produto=p.id " +
                     "GROUP BY p.id, p.nome ORDER BY total_qty DESC LIMIT 10";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getBigDecimal(3)});
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    // Relatório: clientes que mais compraram
    public List<Object[]> relatorioMelhoresClientes() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT c.nome, COUNT(v.id) as total_vendas, SUM(v.valor_final) as total_gasto " +
                     "FROM venda v JOIN cliente c ON v.id_cliente=c.id " +
                     "WHERE v.status='CONCLUIDA' " +
                     "GROUP BY c.id, c.nome ORDER BY total_gasto DESC LIMIT 10";
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getBigDecimal(3)});
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    private Venda mapHeader(ResultSet rs) throws SQLException {
        Venda v = new Venda();
        v.setId(rs.getInt("id"));
        v.setNumero(rs.getString("numero"));
        v.setIdCliente(rs.getInt("id_cliente"));
        v.setNomeCliente(rs.getString("nome_cliente"));
        int idOrc = rs.getInt("id_orcamento");
        if (!rs.wasNull()) v.setIdOrcamento(idOrc);
        v.setDataVenda(LocalDateTime.parse(rs.getString("data_venda")));
        v.setFormaPagamento(rs.getString("forma_pagamento"));
        v.setValorTotal(rs.getBigDecimal("valor_total"));
        v.setDesconto(rs.getBigDecimal("desconto"));
        v.setValorFinal(rs.getBigDecimal("valor_final"));
        v.setStatus(rs.getString("status"));
        v.setObservacao(rs.getString("observacao"));
        return v;
    }
}
