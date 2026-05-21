package dao;

import config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * DAO — requêtes statistiques et rapports de performance.
 * S'appuie sur les vues SQL : v_moyenne_sous_module, v_moyenne_module, v_moyenne_generale.
 */
public class StatistiqueDAO {

    // ── MOYENNE PAR SOUS-MODULE pour un étudiant ──────────────────────────────
    public Map<Long, Double> getMoyennesSousModuleParEtudiant(Long etudiantId) throws SQLException {
        String sql = """
            SELECT sous_module_id, moyenne_sm
              FROM v_moyenne_sous_module
             WHERE etudiant_id = ?
            """;
        Map<Long, Double> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, etudiantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getLong("sous_module_id"), rs.getDouble("moyenne_sm"));
                }
            }
        }
        return result;
    }

    // ── MOYENNE PAR MODULE pour un étudiant ──────────────────────────────────
    public Map<Long, Double> getMoyennesModuleParEtudiant(Long etudiantId) throws SQLException {
        String sql = """
            SELECT module_id, module, moyenne_module
              FROM v_moyenne_module
             WHERE etudiant_id = ?
            """;
        Map<Long, Double> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, etudiantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getLong("module_id"), rs.getDouble("moyenne_module"));
                }
            }
        }
        return result;
    }

    // ── MOYENNE GÉNÉRALE d'un étudiant dans une promotion ────────────────────
    public Optional<Double> getMoyenneGenerale(Long etudiantId, Long promotionId) throws SQLException {
        String sql = """
            SELECT moyenne_generale
              FROM v_moyenne_generale
             WHERE etudiant_id = ? AND promotion_id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, etudiantId);
            ps.setLong(2, promotionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getDouble("moyenne_generale"));
            }
        }
        return Optional.empty();
    }

    // ── CLASSEMENT complet d'une promotion ───────────────────────────────────
    public List<Map<String, Object>> getClassementPromotion(Long promotionId) throws SQLException {
        String sql = """
            SELECT vg.etudiant_id,
                   vg.etudiant,
                   vg.moyenne_generale,
                   RANK() OVER (ORDER BY vg.moyenne_generale DESC) AS rang
              FROM v_moyenne_generale vg
             WHERE vg.promotion_id = ?
             ORDER BY vg.moyenne_generale DESC
            """;
        return executeRapport(sql, promotionId);
    }

    // ── MEILLEUR ÉTUDIANT d'une promotion ────────────────────────────────────
    public Optional<Map<String, Object>> getMeilleurEtudiant(Long promotionId) throws SQLException {
        List<Map<String, Object>> classement = getClassementPromotion(promotionId);
        return classement.isEmpty() ? Optional.empty() : Optional.of(classement.get(0));
    }

    // ── ÉTUDIANTS EN ÉCHEC (moyenne < seuil) ──────────────────────────────────
    public List<Map<String, Object>> getEtudiantsEnEchec(Long promotionId, double seuil) throws SQLException {
        String sql = """
            SELECT vg.etudiant_id,
                   vg.etudiant,
                   vg.moyenne_generale
              FROM v_moyenne_generale vg
             WHERE vg.promotion_id = ? AND vg.moyenne_generale < ?
             ORDER BY vg.moyenne_generale ASC
            """;
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, promotionId);
            ps.setDouble(2, seuil);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    result.add(mapRow(rs, meta));
                }
            }
        }
        return result;
    }

    // ── FICHE RÉSULTAT COMPLÈTE d'un étudiant ────────────────────────────────
    public List<Map<String, Object>> getFicheResultatEtudiant(Long etudiantId,
                                                               Long promotionId) throws SQLException {
        String sql = """
            SELECT m.intitule   AS module,
                   m.coefficient AS coeff_module,
                   sm.intitule  AS sous_module,
                   sm.coefficient AS coeff_sm,
                   n.type_note,
                   n.valeur,
                   vm.moyenne_sm,
                   vmod.moyenne_module
              FROM module m
              JOIN sous_module sm  ON sm.module_id = m.id
              LEFT JOIN note n     ON n.sous_module_id = sm.id
                                  AND n.etudiant_id = ?
              LEFT JOIN v_moyenne_sous_module vm
                     ON vm.sous_module_id = sm.id
                    AND vm.etudiant_id = ?
              LEFT JOIN v_moyenne_module vmod
                     ON vmod.module_id = m.id
                    AND vmod.etudiant_id = ?
             WHERE m.promotion_id = ?
             ORDER BY m.intitule, sm.intitule
            """;
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, etudiantId);
            ps.setLong(2, etudiantId);
            ps.setLong(3, etudiantId);
            ps.setLong(4, promotionId);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) result.add(mapRow(rs, meta));
            }
        }
        return result;
    }

    // ── HELPER PRIVÉ ─────────────────────────────────────────────────────────
    private List<Map<String, Object>> executeRapport(String sql, Long promotionId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, promotionId);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) result.add(mapRow(rs, meta));
            }
        }
        return result;
    }

    private Map<String, Object> mapRow(ResultSet rs, ResultSetMetaData meta) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(meta.getColumnLabel(i), rs.getObject(i));
        }
        return row;
    }
}
