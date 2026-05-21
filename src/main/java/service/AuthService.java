package service;

import model.Utilisateur;

public interface AuthService {
    /** Authentifie l'utilisateur et retourne son objet complet avec rôle. */
    Utilisateur login(String login, String motDePasse);
    void        logout(Long utilisateurId);
    Utilisateur.Role getRole(String login);
}
