package dao;

import config.DatabaseConnection;
import model.Note;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO — tout le SQL lié aux notes est ici, nulle part ailleurs.
 */
public class NoteDAO {

    // ── INSERT ────────────────────────────────────────────────────────────────
    public Note insert(Note n) throws SQLException {
        String sql = """
            INSERT INTO note (valeur, type_note, etudiant_id, sous_module_id, saisi_par)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDouble(1, n.getValeur());
            ps.setString(2, n.getTypeNote().name());
            ps.setLong(3, n.getEtudiantId());
            ps.setLong(4, n.getSousModuleId());
            if (n.getSaisiPar() != null) ps.setLong(5, n.getSaisiPar());
            else ps.setNull(5, Types.BIGINT);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) n.setId(rs.getLong(1));
            }
        }
        return n;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public void update(Note n) throws SQLException {
        String sql = """
            UPDATE note SET valeur = ?, type_note = ?, saisi_par = ?
             WHERE id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, n.getValeur());
            ps.setString(2, n.getTypeNote().name());
            if (n.getSaisiPar() != null) ps.setLong(3, n.getSaisiPar());
            else ps.setNull(3, Types.BIGINT);
            ps.setLong(4, n.getId());

            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM note WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────────
    public Optional<Note> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM note WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    // ── FIND BY ÉTUDIANT ─────────────────────────────────────────────────────
    public List<Note> findByEtudiant(Long etudiantId) throws SQLException {
        String sql = """
            SELECT n.*, sm.intitule AS sm_intitule
              FROM note n
              JOIN sous_module sm ON sm.id = n.sous_module_id
             WHERE n.etudiant_id = ?
             ORDER BY sm.intitule, n.type_note
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, etudiantId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapListAvecJoin(rs);
            }
        }
    }

    // ── FIND BY SOUS-MODULE ───────────────────────────────────────────────────
    public List<Note> findBySousModule(Long sousModuleId) throws SQLException {
        String sql = """
            SELECT n.*,
                   CONCAT(e.nom, ' ', e.prenom) AS etudiant_nom
              FROM note n
              JOIN etudiant e ON e.id = n.etudiant_id
             WHERE n.sous_module_id = ?
             ORDER BY e.nom, e.prenom
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sousModuleId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapListAvecJoin(rs);
            }
        }
    }

    // ── TABLEAU RÉCAPITULATIF par promotion (vue SQL) ─────────────────────────
    public List<Note> findTableauByPromotion(Long promotionId) throws SQLException {
        String sql = """
            SELECT n.*, sm.intitule AS sm_intitule,
                   CONCAT(e.nom, ' ', e.prenom) AS etudiant_nom
              FROM v_tableau_classe v
              JOIN note n ON n.etudiant_id = v.etudiant_id
                         AND n.sous_module_id = v.sous_module_id
              JOIN etudiant e    ON e.id  = n.etudiant_id
              JOIN sous_module sm ON sm.id = n.sous_module_id
             WHERE v.promotion_id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, promotionId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapListAvecJoin(rs);
            }
        }
    }

    // ── UPSERT (saisie ou import — contrainte UNIQUE gère les doublons) ───────
    public void upsert(Note n) throws SQLException {
        String sql = """
            INSERT INTO note (valeur, type_note, etudiant_id, sous_module_id, saisi_par)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE valeur = VALUES(valeur), saisi_par = VALUES(saisi_par)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDouble(1, n.getValeur());
            ps.setString(2, n.getTypeNote().name());
            ps.setLong(3, n.getEtudiantId());
            ps.setLong(4, n.getSousModuleId());
            if (n.getSaisiPar() != null) ps.setLong(5, n.getSaisiPar());
            else ps.setNull(5, Types.BIGINT);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) n.setId(rs.getLong(1));
            }
        }
    }

    // ── IMPORT BATCH ─────────────────────────────────────────────────────────
    public int insertBatch(List<Note> notes) throws SQLException {
        String sql = """
            INSERT INTO note (valeur, type_note, etudiant_id, sous_module_id, saisi_par)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE valeur = VALUES(valeur)
            """;
        int count = 0;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Note n : notes) {
                    ps.setDouble(1, n.getValeur());
                    ps.setString(2, n.getTypeNote().name());
                    ps.setLong(3, n.getEtudiantId());
                    ps.setLong(4, n.getSousModuleId());
                    if (n.getSaisiPar() != null) ps.setLong(5, n.getSaisiPar());
                    else ps.setNull(5, Types.BIGINT);
                    ps.addBatch();
                }
                int[] rows = ps.executeBatch();
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
    private Note map(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getLong("id"));
        n.setValeur(rs.getDouble("valeur"));
        n.setTypeNote(Note.TypeNote.valueOf(rs.getString("type_note")));
        n.setEtudiantId(rs.getLong("etudiant_id"));
        n.setSousModuleId(rs.getLong("sous_module_id"));
        long sp = rs.getLong("saisi_par");
        if (!rs.wasNull()) n.setSaisiPar(sp);
        Timestamp ds = rs.getTimestamp("date_saisie");
        if (ds != null) n.setDateSaisie(ds.toLocalDateTime());
        return n;
    }

    private List<Note> mapListAvecJoin(ResultSet rs) throws SQLException {
        List<Note> list = new ArrayList<>();
        while (rs.next()) {
            Note n = map(rs);
            // champs join optionnels
            try { n.setEtudiantNom(rs.getString("etudiant_nom")); }
            catch (SQLException ignored) {}
            try { n.setSousModuleIntitule(rs.getString("sm_intitule")); }
            catch (SQLException ignored) {}
            list.add(n);
        }
        return list;
    }
}
