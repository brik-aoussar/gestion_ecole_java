package dao;

import config.DatabaseConnection;
import model.Promotion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO — Promotions et Filières. */
public class PromotionDAO {

    // ── INSERT PROMOTION ──────────────────────────────────────────────────────
    public Promotion insert(Promotion p) throws SQLException {
        String sql = """
            INSERT INTO promotion (intitule, annee, filiere_id, actif)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getIntitule());
            ps.setInt(2, p.getAnnee());
            ps.setLong(3, p.getFiliereId());
            ps.setBoolean(4, p.isActif());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getLong(1));
            }
        }
        return p;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public void update(Promotion p) throws SQLException {
        String sql = """
            UPDATE promotion SET intitule = ?, annee = ?, actif = ?
             WHERE id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getIntitule());
            ps.setInt(2, p.getAnnee());
            ps.setBoolean(3, p.isActif());
            ps.setLong(4, p.getId());
            ps.executeUpdate();
        }
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────────
    public Optional<Promotion> findById(Long id) throws SQLException {
        String sql = """
            SELECT p.*, f.intitule AS filiere_intitule
              FROM promotion p
              JOIN filiere f ON f.id = p.filiere_id
             WHERE p.id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────────
    public List<Promotion> findAll() throws SQLException {
        String sql = """
            SELECT p.*, f.intitule AS filiere_intitule
              FROM promotion p
              JOIN filiere f ON f.id = p.filiere_id
             ORDER BY p.annee DESC, p.intitule
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapList(rs);
        }
    }

    // ── FIND BY FILIÈRE ───────────────────────────────────────────────────────
    public List<Promotion> findByFiliere(Long filiereId) throws SQLException {
        String sql = """
            SELECT p.*, f.intitule AS filiere_intitule
              FROM promotion p
              JOIN filiere f ON f.id = p.filiere_id
             WHERE p.filiere_id = ?
             ORDER BY p.annee DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, filiereId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────
    private Promotion map(ResultSet rs) throws SQLException {
        Promotion p = new Promotion();
        p.setId(rs.getLong("id"));
        p.setIntitule(rs.getString("intitule"));
        p.setAnnee(rs.getInt("annee"));
        p.setFiliereId(rs.getLong("filiere_id"));
        p.setActif(rs.getBoolean("actif"));
        try { p.setFiliereIntitule(rs.getString("filiere_intitule")); }
        catch (SQLException ignored) {}
        return p;
    }

    private List<Promotion> mapList(ResultSet rs) throws SQLException {
        List<Promotion> list = new ArrayList<>();
        while (rs.next()) list.add(map(rs));
        return list;
    }
}
