package com.vendas.dao;

import com.vendas.database.DatabaseConnection;
import com.vendas.model.Troca;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TrocaDAO {

    private Connection conn() { return DatabaseConnection.getInstance().getConnection(); }

    private static final String SELECT_BASE =
        "SELECT t.*, v.numero as numero_venda, c.nome as nome_cliente, " +
        "pd.nome as nome_prod_dev, pn.nome as nome_prod_novo " +
        "FROM troca t " +
        "JOIN venda v ON t.id_venda = v.id " +
        "JOIN cliente c ON v.id_cliente = c.id " +
        "JOIN produto pd ON t.id_produto_devolvido = pd.id " +
        "JOIN produto pn ON t.id_produto_novo = pn.id ";

    public List<Troca> findAll() {
        List<Troca> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "ORDER BY t.data_troca DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public Troca findById(int id) {
        try (PreparedStatement ps = conn().prepareStatement(SELECT_BASE + "WHERE t.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public void save(Troca t) {
        String sql = "INSERT INTO troca(numero,id_venda,data_troca,motivo,id_produto_devolvido," +
                     "quantidade_devolvida,id_produto_novo,quantidade_nova,valor_diferenca) VALUES(?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, t.getNumero());
            ps.setInt(2, t.getIdVenda());
            ps.setString(3, t.getDataTroca().toString());
            ps.setString(4, t.getMotivo());
            ps.setInt(5, t.getIdProdutoDevolvido());
            ps.setInt(6, t.getQuantidadeDevolvida());
            ps.setInt(7, t.getIdProdutoNovo());
            ps.setInt(8, t.getQuantidadeNova());
            ps.setBigDecimal(9, t.getValorDiferenca());
            ps.executeUpdate();
            t.setId(lastId());
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private int lastId() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Troca map(ResultSet rs) throws SQLException {
        Troca t = new Troca();
        t.setId(rs.getInt("id"));
        t.setNumero(rs.getString("numero"));
        t.setIdVenda(rs.getInt("id_venda"));
        t.setNumeroVenda(rs.getString("numero_venda"));
        t.setNomeCliente(rs.getString("nome_cliente"));
        t.setDataTroca(LocalDateTime.parse(rs.getString("data_troca")));
        t.setMotivo(rs.getString("motivo"));
        t.setIdProdutoDevolvido(rs.getInt("id_produto_devolvido"));
        t.setNomeProdutoDevolvido(rs.getString("nome_prod_dev"));
        t.setQuantidadeDevolvida(rs.getInt("quantidade_devolvida"));
        t.setIdProdutoNovo(rs.getInt("id_produto_novo"));
        t.setNomeProdutoNovo(rs.getString("nome_prod_novo"));
        t.setQuantidadeNova(rs.getInt("quantidade_nova"));
        t.setValorDiferenca(rs.getBigDecimal("valor_diferenca"));
        return t;
    }
}
