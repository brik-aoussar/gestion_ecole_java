package dao;

import config.DatabaseConnection;
import model.Etudiant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO — tout le SQL lié à l'étudiant est ici, nulle part ailleurs.
 */
public class EtudiantDAO {

    // ── INSERT ────────────────────────────────────────────────────────────────
    public Etudiant insert(Etudiant e) throws SQLException {
        String sql = """
            INSERT INTO etudiant (cne, nom, prenom, date_naissance, email, telephone, statut)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, e.getCne());
            ps.setString(2, e.getNom());
            ps.setString(3, e.getPrenom());
            ps.setDate(4, e.getDateNaissance() != null
                          ? Date.valueOf(e.getDateNaissance()) : null);
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getTelephone());
            ps.setString(7, e.getStatut().name());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) e.setId(rs.getLong(1));
            }
        }
        return e;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public void update(Etudiant e) throws SQLException {
        String sql = """
            UPDATE etudiant
               SET cne = ?, nom = ?, prenom = ?, date_naissance = ?,
                   email = ?, telephone = ?, statut = ?
             WHERE id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getCne());
            ps.setString(2, e.getNom());
            ps.setString(3, e.getPrenom());
            ps.setDate(4, e.getDateNaissance() != null
                          ? Date.valueOf(e.getDateNaissance()) : null);
            ps.setString(5, e.getEmail());
            ps.setString(6, e.getTelephone());
            ps.setString(7, e.getStatut().name());
            ps.setLong(8, e.getId());

            ps.executeUpdate();
        }
    }

    // ── ARCHIVER (soft delete) ────────────────────────────────────────────────
    public void archiver(Long id) throws SQLException {
        String sql = "UPDATE etudiant SET statut = 'ARCHIVE' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────────
    public Optional<Etudiant> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM etudiant WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    // ── FIND BY CNE ───────────────────────────────────────────────────────────
    public Optional<Etudiant> findByCne(String cne) throws SQLException {
        String sql = "SELECT * FROM etudiant WHERE cne = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cne);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    // ── FIND ALL ACTIFS ───────────────────────────────────────────────────────
    public List<Etudiant> findAllActifs() throws SQLException {
        String sql = "SELECT * FROM etudiant WHERE statut = 'ACTIF' ORDER BY nom, prenom";
        return executeList(sql);
    }

    // ── FIND BY PROMOTION ─────────────────────────────────────────────────────
    public List<Etudiant> findByPromotion(Long promotionId) throws SQLException {
        String sql = """
            SELECT e.*
              FROM etudiant e
              JOIN inscription i ON i.etudiant_id = e.id
             WHERE i.promotion_id = ?
               AND e.statut = 'ACTIF'
             ORDER BY e.nom, e.prenom
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, promotionId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    // ── RECHERCHE (nom ou prenom ou CNE) ─────────────────────────────────────
    public List<Etudiant> rechercher(String terme) throws SQLException {
        String sql = """
            SELECT * FROM etudiant
             WHERE (nom LIKE ? OR prenom LIKE ? OR cne LIKE ?)
               AND statut = 'ACTIF'
             ORDER BY nom, prenom
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + terme + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                return mapList(rs);
            }
        }
    }

    // ── INSCRIRE à une promotion ──────────────────────────────────────────────
    public void inscrireAPromotion(Long etudiantId, Long promotionId) throws SQLException {
        String sql = """
            INSERT IGNORE INTO inscription (etudiant_id, promotion_id)
            VALUES (?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, etudiantId);
            ps.setLong(2, promotionId);
            ps.executeUpdate();
        }
    }

    // ── IMPORT BATCH depuis Excel ─────────────────────────────────────────────
    public int insertBatch(List<Etudiant> etudiants, Long promotionId) throws SQLException {
        String sqlEtudiant = """
            INSERT INTO etudiant (cne, nom, prenom, date_naissance, email, statut)
            VALUES (?, ?, ?, ?, ?, 'ACTIF')
            ON DUPLICATE KEY UPDATE nom=VALUES(nom), prenom=VALUES(prenom)
            """;
        String sqlInscription = "INSERT IGNORE INTO inscription (etudiant_id, promotion_id) VALUES (?, ?)";

        int count = 0;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psEt  = conn.prepareStatement(sqlEtudiant, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psIns = conn.prepareStatement(sqlInscription)) {

                for (Etudiant e : etudiants) {
                    psEt.setString(1, e.getCne());
                    psEt.setString(2, e.getNom());
                    psEt.setString(3, e.getPrenom());
                    psEt.setDate(4, e.getDateNaissance() != null
                                    ? Date.valueOf(e.getDateNaissance()) : null);
                    psEt.setString(5, e.getEmail());
                    psEt.addBatch();
                }

                psEt.executeBatch();

                try (ResultSet keys = psEt.getGeneratedKeys()) {
                    int i = 0;
                    while (keys.next()) {
                        long etId = keys.getLong(1);
                        etudiants.get(i++).setId(etId);
                        psIns.setLong(1, etId);
                        psIns.setLong(2, promotionId);
                        psIns.addBatch();
                    }
                }

                int[] rows = psIns.executeBatch();
                for (int r : rows) count += r;

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return count;
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────
    private Etudiant map(ResultSet rs) throws SQLException {
        Etudiant e = new Etudiant();
        e.setId(rs.getLong("id"));
        e.setCne(rs.getString("cne"));
        e.setNom(rs.getString("nom"));
        e.setPrenom(rs.getString("prenom"));
        Date d = rs.getDate("date_naissance");
        if (d != null) e.setDateNaissance(d.toLocalDate());
        e.setEmail(rs.getString("email"));
        e.setTelephone(rs.getString("telephone"));
        e.setStatut(Etudiant.Statut.valueOf(rs.getString("statut")));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) e.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) e.setUpdatedAt(ua.toLocalDateTime());
        return e;
    }

    private List<Etudiant> mapList(ResultSet rs) throws SQLException {
        List<Etudiant> list = new ArrayList<>();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    private List<Etudiant> executeList(String sql) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapList(rs);
        }
    }
}
