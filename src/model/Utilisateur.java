package model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * POJO abstrait — représente tout utilisateur du système.
 * Héritage par table concrète (responsable_planning, enseignant…).
 */
public abstract class Utilisateur {

    public enum Role {
        RESPONSABLE_PLANNING,
        RESPONSABLE_FILIERE,
        ENSEIGNANT
    }

    private Long          id;
    private String        login;
    private String        motDePasse;   // bcrypt hash
    private String        nom;
    private String        prenom;
    private String        email;
    private Role          role;
    private boolean       actif;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructeurs ────────────────────────────────────────────────────────
    protected Utilisateur() {}

    protected Utilisateur(String login, String motDePasse,
                          String nom, String prenom,
                          String email, Role role) {
        this.login      = login;
        this.motDePasse = motDePasse;
        this.nom        = nom;
        this.prenom     = prenom;
        this.email      = email;
        this.role       = role;
        this.actif      = true;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getLogin()                   { return login; }
    public void setLogin(String login)         { this.login = login; }

    public String getMotDePasse()              { return motDePasse; }
    public void setMotDePasse(String mdp)      { this.motDePasse = mdp; }

    public String getNom()                     { return nom; }
    public void setNom(String nom)             { this.nom = nom; }

    public String getPrenom()                  { return prenom; }
    public void setPrenom(String prenom)       { this.prenom = prenom; }

    public String getEmail()                   { return email; }
    public void setEmail(String email)         { this.email = email; }

    public Role getRole()                      { return role; }
    public void setRole(Role role)             { this.role = role; }

    public boolean isActif()                   { return actif; }
    public void setActif(boolean actif)        { this.actif = actif; }

    public LocalDateTime getCreatedAt()        { return createdAt; }
    public void setCreatedAt(LocalDateTime t)  { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()        { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)  { this.updatedAt = t; }

    public String getNomComplet()              { return nom + " " + prenom; }

    // ── equals / hashCode / toString ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Utilisateur u)) return false;
        return Objects.equals(id, u.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
               "{id=" + id + ", login='" + login + "', role=" + role + "}";
    }
}
