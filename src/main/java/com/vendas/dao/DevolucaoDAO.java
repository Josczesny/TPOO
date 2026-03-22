package com.vendas.dao;

import com.vendas.database.DatabaseConnection;
import com.vendas.model.Devolucao;
import com.vendas.model.ItemDevolucao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DevolucaoDAO {

    private Connection conn() { return DatabaseConnection.getInstance().getConnection(); }

    private static final String SELECT_BASE =
        "SELECT d.*, v.numero as numero_venda, c.nome as nome_cliente " +
        "FROM devolucao d " +
        "JOIN venda v ON d.id_venda = v.id " +
        "JOIN cliente c ON v.id_cliente = c.id ";

    public List<Devolucao> findAll() {
        List<Devolucao> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "ORDER BY d.data_devolucao DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapHeader(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public Devolucao findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "WHERE d.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Devolucao d = mapHeader(rs);
                    d.setItens(findItens(id));
                    return d;
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public List<ItemDevolucao> findItens(int idDevolucao) {
        List<ItemDevolucao> list = new ArrayList<>();
        String sql = "SELECT id.*, p.nome as nome_produto FROM item_devolucao id " +
                     "JOIN produto p ON id.id_produto = p.id WHERE id.id_devolucao=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, idDevolucao);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemDevolucao item = new ItemDevolucao();
                    item.setId(rs.getInt("id"));
                    item.setIdDevolucao(rs.getInt("id_devolucao"));
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

    public void save(Devolucao d) {
        String sql = "INSERT INTO devolucao(numero,id_venda,data_devolucao,motivo,valor_devolvido) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, d.getNumero());
            ps.setInt(2, d.getIdVenda());
            ps.setString(3, d.getDataDevolucao().toString());
            ps.setString(4, d.getMotivo());
            ps.setBigDecimal(5, d.getValorDevolvido());
            ps.executeUpdate();
            d.setId(lastId());
            for (ItemDevolucao item : d.getItens()) {
                item.setIdDevolucao(d.getId());
                saveItem(item);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void saveItem(ItemDevolucao item) throws SQLException {
        String sql = "INSERT INTO item_devolucao(id_devolucao,id_produto,quantidade,preco_unitario,subtotal) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, item.getIdDevolucao());
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

    private Devolucao mapHeader(ResultSet rs) throws SQLException {
        Devolucao d = new Devolucao();
        d.setId(rs.getInt("id"));
        d.setNumero(rs.getString("numero"));
        d.setIdVenda(rs.getInt("id_venda"));
        d.setNumeroVenda(rs.getString("numero_venda"));
        d.setNomeCliente(rs.getString("nome_cliente"));
        d.setDataDevolucao(LocalDateTime.parse(rs.getString("data_devolucao")));
        d.setMotivo(rs.getString("motivo"));
        d.setValorDevolvido(rs.getBigDecimal("valor_devolvido"));
        return d;
    }
}
