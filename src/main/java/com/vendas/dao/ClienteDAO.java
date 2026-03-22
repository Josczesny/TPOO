package com.vendas.dao;

import com.vendas.database.DatabaseConnection;
import com.vendas.model.Cliente;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    private Connection conn() { return DatabaseConnection.getInstance().getConnection(); }

    public List<Cliente> findAll() {
        List<Cliente> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM cliente ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public Cliente findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM cliente WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public List<Cliente> findByNomeOrCpf(String termo) {
        List<Cliente> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM cliente WHERE nome LIKE ? OR cpf LIKE ? ORDER BY nome")) {
            ps.setString(1, "%" + termo + "%");
            ps.setString(2, "%" + termo + "%");
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public void save(Cliente c) {
        String sql = "INSERT INTO cliente(nome,cpf,email,telefone,endereco,data_cadastro) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getCpf());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getTelefone());
            ps.setString(5, c.getEndereco());
            ps.setString(6, c.getDataCadastro().toString());
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

    public void update(Cliente c) {
        String sql = "UPDATE cliente SET nome=?,cpf=?,email=?,telefone=?,endereco=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getCpf());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getTelefone());
            ps.setString(5, c.getEndereco());
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void delete(int id) {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM cliente WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Cliente map(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNome(rs.getString("nome"));
        c.setCpf(rs.getString("cpf"));
        c.setEmail(rs.getString("email"));
        c.setTelefone(rs.getString("telefone"));
        c.setEndereco(rs.getString("endereco"));
        String dt = rs.getString("data_cadastro");
        if (dt != null) c.setDataCadastro(LocalDate.parse(dt));
        return c;
    }
}
