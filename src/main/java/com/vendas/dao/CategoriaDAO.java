package com.vendas.dao;

import com.vendas.database.DatabaseConnection;
import com.vendas.model.Categoria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    private Connection conn() { return DatabaseConnection.getInstance().getConnection(); }

    public List<Categoria> findAll() {
        List<Categoria> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM categoria ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public Categoria findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM categoria WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public void save(Categoria c) {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO categoria(nome, descricao) VALUES(?,?)")) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getDescricao());
            ps.executeUpdate();
            c.setId(lastId());
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private int lastId() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public void update(Categoria c) {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE categoria SET nome=?, descricao=? WHERE id=?")) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getDescricao());
            ps.setInt(3, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void delete(int id) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM categoria WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Categoria map(ResultSet rs) throws SQLException {
        return new Categoria(rs.getInt("id"), rs.getString("nome"), rs.getString("descricao"));
    }
}
