package dao;

import config.DatabaseConnection;
import model.*;
import model.Utilisateur.Role;

import java.sql.*;
import java.util.Optional;

/**
 * DAO — authentification et gestion des comptes utilisateurs.
 * Gère les 3 sous-tables : responsable_planning, responsable_filiere, enseignant.
 */
public class UtilisateurDAO {

    // ── FIND BY LOGIN (pour l'authentification) ───────────────────────────────
    public Optional<Utilisateur> findByLogin(String login) throws SQLException {
        String sql = """
            SELECT u.*,
                   rp.matricule  AS rp_mat,  rp.departement,
                   rf.matricule  AS rf_mat,  rf.filiere_id,
                   e.matricule   AS e_mat,   e.specialite,  e.grade
              FROM utilisateur u
              LEFT JOIN responsable_planning rp ON rp.id = u.id
              LEFT JOIN responsable_filiere  rf ON rf.id = u.id
              LEFT JOIN enseignant           e  ON e.id  = u.id
             WHERE u.login = ? AND u.actif = TRUE
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUtilisateur(rs));
            }
        }
        return Optional.empty();
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────────
    public Optional<Utilisateur> findById(Long id) throws SQLException {
        String sql = """
            SELECT u.*,
                   rp.matricule AS rp_mat,  rp.departement,
                   rf.matricule AS rf_mat,  rf.filiere_id,
                   e.matricule  AS e_mat,   e.specialite,  e.grade
              FROM utilisateur u
              LEFT JOIN responsable_planning rp ON rp.id = u.id
              LEFT JOIN responsable_filiere  rf ON rf.id = u.id
              LEFT JOIN enseignant           e  ON e.id  = u.id
             WHERE u.id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUtilisateur(rs));
            }
        }
        return Optional.empty();
    }

    // ── INSERT ENSEIGNANT ─────────────────────────────────────────────────────
    public Enseignant insertEnseignant(Enseignant ens) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long uid = insertUtilisateurBase(conn, ens);
                ens.setId(uid);

                String sql = "INSERT INTO enseignant (id, matricule, specialite, grade) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, uid);
                    ps.setString(2, ens.getMatricule());
                    ps.setString(3, ens.getSpecialite());
                    ps.setString(4, ens.getGrade().name());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return ens;
    }

    // ── INSERT RESPONSABLE FILIÈRE ────────────────────────────────────────────
    public ResponsableFiliere insertResponsableFiliere(ResponsableFiliere rf) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long uid = insertUtilisateurBase(conn, rf);
                rf.setId(uid);

                String sql = "INSERT INTO responsable_filiere (id, matricule, filiere_id) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, uid);
                    ps.setString(2, rf.getMatricule());
                    ps.setLong(3, rf.getFiliereId());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return rf;
    }

    // ── CHANGER MOT DE PASSE ──────────────────────────────────────────────────
    public void updateMotDePasse(Long id, String hashedPassword) throws SQLException {
        String sql = "UPDATE utilisateur SET mot_de_passe = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    // ── DÉSACTIVER COMPTE ─────────────────────────────────────────────────────
    public void desactiver(Long id) throws SQLException {
        String sql = "UPDATE utilisateur SET actif = FALSE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ── HELPER PRIVÉ : insérer dans la table utilisateur ─────────────────────
    private long insertUtilisateurBase(Connection conn, Utilisateur u) throws SQLException {
        String sql = """
            INSERT INTO utilisateur (login, mot_de_passe, nom, prenom, email, role, actif)
            VALUES (?, ?, ?, ?, ?, ?, TRUE)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getLogin());
            ps.setString(2, u.getMotDePasse());
            ps.setString(3, u.getNom());
            ps.setString(4, u.getPrenom());
            ps.setString(5, u.getEmail());
            ps.setString(6, u.getRole().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
                throw new SQLException("Échec génération ID utilisateur");
            }
        }
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────
    private Utilisateur mapUtilisateur(ResultSet rs) throws SQLException {
        Role role = Role.valueOf(rs.getString("role"));

        return switch (role) {
            case RESPONSABLE_PLANNING -> {
                ResponsablePlanning rp = new ResponsablePlanning();
                fillBase(rp, rs);
                rp.setMatricule(rs.getString("rp_mat"));
                rp.setDepartement(rs.getString("departement"));
                yield rp;
            }
            case RESPONSABLE_FILIERE -> {
                ResponsableFiliere rf = new ResponsableFiliere();
                fillBase(rf, rs);
                rf.setMatricule(rs.getString("rf_mat"));
                rf.setFiliereId(rs.getLong("filiere_id"));
                yield rf;
            }
            case ENSEIGNANT -> {
                Enseignant e = new Enseignant();
                fillBase(e, rs);
                e.setMatricule(rs.getString("e_mat"));
                e.setSpecialite(rs.getString("specialite"));
                e.setGrade(Enseignant.Grade.valueOf(rs.getString("grade")));
                yield e;
            }
        };
    }

    private void fillBase(Utilisateur u, ResultSet rs) throws SQLException {
        u.setId(rs.getLong("id"));
        u.setLogin(rs.getString("login"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActif(rs.getBoolean("actif"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) u.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) u.setUpdatedAt(ua.toLocalDateTime());
    }
}
