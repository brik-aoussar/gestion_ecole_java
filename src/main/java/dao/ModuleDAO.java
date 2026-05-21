package dao;

import config.DatabaseConnection;
import model.Module;
import model.SousModule;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO — SQL pour Module et SousModule (même package pédagogique).
 */
public class ModuleDAO {

    // ══════════════════════════════════════════════════════════════════════════
    //  MODULE
    // ══════════════════════════════════════════════════════════════════════════

    public Module insertModule(Module m) throws SQLException {
        String sql = """
            INSERT INTO module (code, intitule, coefficient, promotion_id)
            VALUES (?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, m.getCode());
            ps.setString(2, m.getIntitule());
            ps.setDouble(3, m.getCoefficient());
            ps.setLong(4, m.getPromotionId());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) m.setId(rs.getLong(1));
            }
        }
        return m;
    }

    public void updateModule(Module m) throws SQLException {
        String sql = """
            UPDATE module SET code = ?, intitule = ?, coefficient = ?
             WHERE id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getCode());
            ps.setString(2, m.getIntitule());
            ps.setDouble(3, m.getCoefficient());
            ps.setLong(4, m.getId());
            ps.executeUpdate();
        }
    }

    public void deleteModule(Long id) throws SQLException {
        String sql = "DELETE FROM module WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<Module> findModuleById(Long id) throws SQLException {
        String sql = "SELECT * FROM module WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapModule(rs));
            }
        }
        return Optional.empty();
    }

    public List<Module> findModulesByPromotion(Long promotionId) throws SQLException {
        String sql = "SELECT * FROM module WHERE promotion_id = ? ORDER BY intitule";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, promotionId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapModuleList(rs);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SOUS-MODULE
    // ══════════════════════════════════════════════════════════════════════════

    public SousModule insertSousModule(SousModule sm) throws SQLException {
        String sql = """
            INSERT INTO sous_module (code, intitule, coefficient, module_id, enseignant_id)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sm.getCode());
            ps.setString(2, sm.getIntitule());
            ps.setDouble(3, sm.getCoefficient());
            ps.setLong(4, sm.getModuleId());
            if (sm.getEnseignantId() != null) ps.setLong(5, sm.getEnseignantId());
            else ps.setNull(5, Types.BIGINT);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) sm.setId(rs.getLong(1));
            }
        }
        return sm;
    }

    public void updateSousModule(SousModule sm) throws SQLException {
        String sql = """
            UPDATE sous_module
               SET code = ?, intitule = ?, coefficient = ?, enseignant_id = ?
             WHERE id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sm.getCode());
            ps.setString(2, sm.getIntitule());
            ps.setDouble(3, sm.getCoefficient());
            if (sm.getEnseignantId() != null) ps.setLong(4, sm.getEnseignantId());
            else ps.setNull(4, Types.BIGINT);
            ps.setLong(5, sm.getId());
            ps.executeUpdate();
        }
    }

    public void deleteSousModule(Long id) throws SQLException {
        String sql = "DELETE FROM sous_module WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public Optional<SousModule> findSousModuleById(Long id) throws SQLException {
        String sql = """
            SELECT sm.*,
                   CONCAT(u.nom, ' ', u.prenom) AS enseignant_nom
              FROM sous_module sm
              LEFT JOIN enseignant e ON e.id = sm.enseignant_id
              LEFT JOIN utilisateur u ON u.id = e.id
             WHERE sm.id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapSousModule(rs));
            }
        }
        return Optional.empty();
    }

    public List<SousModule> findByModule(Long moduleId) throws SQLException {
        String sql = """
            SELECT sm.*,
                   CONCAT(u.nom, ' ', u.prenom) AS enseignant_nom
              FROM sous_module sm
              LEFT JOIN enseignant e ON e.id = sm.enseignant_id
              LEFT JOIN utilisateur u ON u.id = e.id
             WHERE sm.module_id = ?
             ORDER BY sm.intitule
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, moduleId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapSousModuleList(rs);
            }
        }
    }

    /** Sous-modules assignés à un enseignant spécifique. */
    public List<SousModule> findByEnseignant(Long enseignantId) throws SQLException {
        String sql = """
            SELECT sm.*, NULL AS enseignant_nom
              FROM sous_module sm
             WHERE sm.enseignant_id = ?
             ORDER BY sm.intitule
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enseignantId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapSousModuleList(rs);
            }
        }
    }

    /** Assigner / changer l'enseignant d'un sous-module. */
    public void assignerEnseignant(Long sousModuleId, Long enseignantId) throws SQLException {
        String sql = "UPDATE sous_module SET enseignant_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (enseignantId != null) ps.setLong(1, enseignantId);
            else ps.setNull(1, Types.BIGINT);
            ps.setLong(2, sousModuleId);
            ps.executeUpdate();
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────
    private Module mapModule(ResultSet rs) throws SQLException {
        Module m = new Module();
        m.setId(rs.getLong("id"));
        m.setCode(rs.getString("code"));
        m.setIntitule(rs.getString("intitule"));
        m.setCoefficient(rs.getDouble("coefficient"));
        m.setPromotionId(rs.getLong("promotion_id"));
        return m;
    }

    private List<Module> mapModuleList(ResultSet rs) throws SQLException {
        List<Module> list = new ArrayList<>();
        while (rs.next()) list.add(mapModule(rs));
        return list;
    }

    private SousModule mapSousModule(ResultSet rs) throws SQLException {
        SousModule sm = new SousModule();
        sm.setId(rs.getLong("id"));
        sm.setCode(rs.getString("code"));
        sm.setIntitule(rs.getString("intitule"));
        sm.setCoefficient(rs.getDouble("coefficient"));
        sm.setModuleId(rs.getLong("module_id"));
        long eid = rs.getLong("enseignant_id");
        if (!rs.wasNull()) sm.setEnseignantId(eid);
        try { sm.setEnseignantNom(rs.getString("enseignant_nom")); }
        catch (SQLException ignored) {}
        return sm;
    }

    private List<SousModule> mapSousModuleList(ResultSet rs) throws SQLException {
        List<SousModule> list = new ArrayList<>();
        while (rs.next()) list.add(mapSousModule(rs));
        return list;
    }
}
