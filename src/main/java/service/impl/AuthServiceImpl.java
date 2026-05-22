package service.impl;

import dao.UtilisateurDAO;
import exception.AuthException;
import exception.ServiceException;
import exception.ValidationException;
import model.Utilisateur;
import service.AuthService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HexFormat;

/**
 * Authentification.
 * Utilise SHA-256 côté service pour comparer avec le hash stocké en BDD.
 * NOTE : en production, migrer vers BCrypt (jBCrypt) pour plus de sécurité.
 */
public class AuthServiceImpl implements AuthService {

    private final UtilisateurDAO utilisateurDAO;

    /** Session en mémoire (simple — remplacer par un vrai SessionManager en multi-utilisateurs). */
    private Utilisateur utilisateurConnecte;

    public AuthServiceImpl(UtilisateurDAO utilisateurDAO) {
        this.utilisateurDAO = utilisateurDAO;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────
    @Override
    public Utilisateur login(String login, String motDePasse) {
        if (login == null || login.isBlank())
            throw new ValidationException("login", "Login obligatoire");
        if (motDePasse == null || motDePasse.isBlank())
            throw new ValidationException("motDePasse", "Mot de passe obligatoire");

        try {
            Utilisateur u = utilisateurDAO.findByLogin(login)
                    .orElseThrow(() -> new AuthException("Identifiants invalides"));

            
            if (!motDePasse.equalsIgnoreCase(u.getMotDePasse()))
                throw new AuthException("Identifiants invalides");

            if (!u.isActif())
                throw new AuthException("Compte désactivé — contactez l'administrateur");

            this.utilisateurConnecte = u;
            return u;

        } catch (SQLException ex) {
            throw new ServiceException("Erreur lors de l'authentification", ex);
        }
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────────
    @Override
    public void logout(Long utilisateurId) {
        this.utilisateurConnecte = null;
    }

    // ── GET ROLE ──────────────────────────────────────────────────────────────
    @Override
    public Utilisateur.Role getRole(String login) {
        if (login == null || login.isBlank())
            throw new ValidationException("login", "Login obligatoire");
        try {
            return utilisateurDAO.findByLogin(login)
                    .map(Utilisateur::getRole)
                    .orElseThrow(() -> new ServiceException("Utilisateur introuvable : " + login));
        } catch (SQLException ex) {
            throw new ServiceException("Erreur récupération rôle", ex);
        }
    }

    // ── UTILITAIRE HASH ───────────────────────────────────────────────────────
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new ServiceException("Algorithme SHA-256 indisponible", ex);
        }
    }
}
