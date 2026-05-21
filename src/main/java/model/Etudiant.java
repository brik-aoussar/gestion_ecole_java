package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Étudiant inscrit dans une ou plusieurs promotions. */
public class Etudiant {

    public enum Statut { ACTIF, ARCHIVE, SUSPENDU }

    private Long          id;
    private String        cne;
    private String        nom;
    private String        prenom;
    private LocalDate     dateNaissance;
    private String        email;
    private String        telephone;
    private Statut        statut;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Constructeurs ────────────────────────────────────────────────────────
    public Etudiant() {}

    public Etudiant(String cne, String nom, String prenom,
                    LocalDate dateNaissance, String email) {
        this.cne           = cne;
        this.nom           = nom;
        this.prenom        = prenom;
        this.dateNaissance = dateNaissance;
        this.email         = email;
        this.statut        = Statut.ACTIF;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public String getCne()                           { return cne; }
    public void setCne(String cne)                   { this.cne = cne; }

    public String getNom()                           { return nom; }
    public void setNom(String nom)                   { this.nom = nom; }

    public String getPrenom()                        { return prenom; }
    public void setPrenom(String prenom)             { this.prenom = prenom; }

    public LocalDate getDateNaissance()              { return dateNaissance; }
    public void setDateNaissance(LocalDate d)        { this.dateNaissance = d; }

    public String getEmail()                         { return email; }
    public void setEmail(String email)               { this.email = email; }

    public String getTelephone()                     { return telephone; }
    public void setTelephone(String telephone)       { this.telephone = telephone; }

    public Statut getStatut()                        { return statut; }
    public void setStatut(Statut statut)             { this.statut = statut; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()              { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)        { this.updatedAt = t; }

    public String getNomComplet()                    { return nom + " " + prenom; }
    public boolean isActif()                         { return statut == Statut.ACTIF; }

    // ── equals / hashCode / toString ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Etudiant e)) return false;
        return Objects.equals(id, e.id) || Objects.equals(cne, e.cne);
    }

    @Override
    public int hashCode() { return Objects.hash(id, cne); }

    @Override
    public String toString() {
        return "Etudiant{id=" + id + ", cne='" + cne +
               "', nom='" + nom + "', prenom='" + prenom +
               "', statut=" + statut + "}";
    }
}
