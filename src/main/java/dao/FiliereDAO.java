package dao;

import config.DatabaseConnection;
import model.Filiere;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO — Filières. */
public class FiliereDAO {

    public Filiere insert(Filiere f) throws SQLException {
        String sql = "INSERT INTO filiere (code, intitule, domaine) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getCode());
            ps.setString(2, f.getIntitule());
            ps.setString(3, f.getDomaine());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) f.setId(rs.getLong(1));
            }
        }
        return f;
    }

    public void update(Filiere f) throws SQLException {
        String sql = "UPDATE filiere SET code=?, intitule=?, domaine=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.getCode());
            ps.setString(2, f.getIntitule());
            ps.setString(3, f.getDomaine());
            ps.setLong(4, f.getId());
            ps.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM filiere WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Filiere> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM filiere WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Filiere> findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM filiere WHERE code=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    public List<Filiere> findAll() throws SQLException {
        String sql = "SELECT * FROM filiere ORDER BY intitule";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Filiere> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    /** Filières gérées par un responsable_filiere donné. */
    public List<Filiere> findByResponsable(Long responsableFiliereId) throws SQLException {
        String sql = """
            SELECT f.*
              FROM filiere f
              JOIN responsable_filiere rf ON rf.filiere_id = f.id
             WHERE rf.id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, responsableFiliereId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Filiere> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    /** Vérifie si la filière a des promotions actives (avant suppression). */
    public boolean hasPromotionsActives(Long filiereId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM promotion WHERE filiere_id=? AND actif=TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, filiereId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Filiere map(ResultSet rs) throws SQLException {
        Filiere f = new Filiere();
        f.setId(rs.getLong("id"));
        f.setCode(rs.getString("code"));
        f.setIntitule(rs.getString("intitule"));
        f.setDomaine(rs.getString("domaine"));
        return f;
    }
}
