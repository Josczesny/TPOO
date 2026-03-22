package com.vendas.dao;

import com.vendas.database.DatabaseConnection;
import com.vendas.model.Produto;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    private Connection conn() { return DatabaseConnection.getInstance().getConnection(); }

    private static final String SELECT_BASE =
        "SELECT p.*, c.nome as nome_categoria FROM produto p " +
        "LEFT JOIN categoria c ON p.id_categoria = c.id ";

    public List<Produto> findAll() {
        List<Produto> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "ORDER BY p.nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public Produto findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "WHERE p.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public List<Produto> findByNomeOrCodigo(String termo) {
        List<Produto> list = new ArrayList<>();
        String sql = SELECT_BASE + "WHERE p.nome LIKE ? OR p.codigo LIKE ? ORDER BY p.nome";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, "%" + termo + "%");
            ps.setString(2, "%" + termo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public void save(Produto p) {
        String sql = "INSERT INTO produto(codigo,nome,descricao,preco,estoque,estoque_minimo,id_categoria) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNome());
            ps.setString(3, p.getDescricao());
            ps.setBigDecimal(4, p.getPreco());
            ps.setInt(5, p.getEstoque());
            ps.setInt(6, p.getEstoqueMinimo());
            if (p.getIdCategoria() > 0) ps.setInt(7, p.getIdCategoria()); else ps.setNull(7, Types.INTEGER);
            ps.executeUpdate();
            p.setId(lastId());
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private int lastId() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void update(Produto p) {
        String sql = "UPDATE produto SET codigo=?,nome=?,descricao=?,preco=?,estoque=?,estoque_minimo=?,id_categoria=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNome());
            ps.setString(3, p.getDescricao());
            ps.setBigDecimal(4, p.getPreco());
            ps.setInt(5, p.getEstoque());
            ps.setInt(6, p.getEstoqueMinimo());
            if (p.getIdCategoria() > 0) ps.setInt(7, p.getIdCategoria()); else ps.setNull(7, Types.INTEGER);
            ps.setInt(8, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void atualizarEstoque(int idProduto, int novoEstoque) {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE produto SET estoque=? WHERE id=?")) {
            ps.setInt(1, novoEstoque);
            ps.setInt(2, idProduto);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void delete(int id) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM produto WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Produto map(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setNome(rs.getString("nome"));
        p.setDescricao(rs.getString("descricao"));
        p.setPreco(rs.getBigDecimal("preco"));
        p.setEstoque(rs.getInt("estoque"));
        p.setEstoqueMinimo(rs.getInt("estoque_minimo"));
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setNomeCategoria(rs.getString("nome_categoria"));
        return p;
    }
}
