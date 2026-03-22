package com.vendas.dao;

import com.vendas.database.DatabaseConnection;
import com.vendas.model.ItemOrcamento;
import com.vendas.model.Orcamento;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrcamentoDAO {

    private Connection conn() { return DatabaseConnection.getInstance().getConnection(); }

    private static final String SELECT_BASE =
        "SELECT o.*, c.nome as nome_cliente FROM orcamento o " +
        "JOIN cliente c ON o.id_cliente = c.id ";

    public List<Orcamento> findAll() {
        List<Orcamento> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "ORDER BY o.data_emissao DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapHeader(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public Orcamento findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "WHERE o.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Orcamento o = mapHeader(rs);
                    o.setItens(findItens(id));
                    return o;
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public List<Orcamento> findPendentes() {
        List<Orcamento> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "WHERE o.status = 'PENDENTE' ORDER BY o.data_validade")) {
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapHeader(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public List<ItemOrcamento> findItens(int idOrcamento) {
        List<ItemOrcamento> list = new ArrayList<>();
        String sql = "SELECT io.*, p.nome as nome_produto FROM item_orcamento io " +
                     "JOIN produto p ON io.id_produto = p.id WHERE io.id_orcamento=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idOrcamento);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemOrcamento item = new ItemOrcamento();
                    item.setId(rs.getInt("id"));
                    item.setIdOrcamento(rs.getInt("id_orcamento"));
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

    public void save(Orcamento o) {
        String sql = "INSERT INTO orcamento(numero,id_cliente,data_emissao,data_validade,status,valor_total,observacao) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, o.getNumero());
            ps.setInt(2, o.getIdCliente());
            ps.setString(3, o.getDataEmissao().toString());
            ps.setString(4, o.getDataValidade().toString());
            ps.setString(5, o.getStatus());
            ps.setBigDecimal(6, o.getValorTotal());
            ps.setString(7, o.getObservacao());
            ps.executeUpdate();
            o.setId(lastId());
            for (ItemOrcamento item : o.getItens()) {
                item.setIdOrcamento(o.getId());
                saveItem(item);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void saveItem(ItemOrcamento item) throws SQLException {
        String sql = "INSERT INTO item_orcamento(id_orcamento,id_produto,quantidade,preco_unitario,subtotal) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, item.getIdOrcamento());
            ps.setInt(2, item.getIdProduto());
            ps.setInt(3, item.getQuantidade());
            ps.setBigDecimal(4, item.getPrecoUnitario());
            ps.setBigDecimal(5, item.getSubtotal());
            ps.executeUpdate();
            item.setId(lastId());
        }
    }

    private int lastId() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void updateStatus(int id, String status) {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE orcamento SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void atualizarOrcamentosVencidos() {
        String sql = "UPDATE orcamento SET status='VENCIDO' WHERE status='PENDENTE' AND data_validade < ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Orcamento mapHeader(ResultSet rs) throws SQLException {
        Orcamento o = new Orcamento();
        o.setId(rs.getInt("id"));
        o.setNumero(rs.getString("numero"));
        o.setIdCliente(rs.getInt("id_cliente"));
        o.setNomeCliente(rs.getString("nome_cliente"));
        o.setDataEmissao(LocalDate.parse(rs.getString("data_emissao")));
        o.setDataValidade(LocalDate.parse(rs.getString("data_validade")));
        o.setStatus(rs.getString("status"));
        o.setValorTotal(rs.getBigDecimal("valor_total"));
        o.setObservacao(rs.getString("observacao"));
        return o;
    }
}
