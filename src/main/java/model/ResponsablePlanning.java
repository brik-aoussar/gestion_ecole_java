package model;

import java.util.Objects;

/** Responsable planning & évaluation — accès complet. */
public class ResponsablePlanning extends Utilisateur {

    private String matricule;
    private String departement;

    // ── Constructeurs ────────────────────────────────────────────────────────
    public ResponsablePlanning() { super(); }

    public ResponsablePlanning(String login, String motDePasse,
                               String nom, String prenom, String email,
                               String matricule, String departement) {
        super(login, motDePasse, nom, prenom, email, Role.RESPONSABLE_PLANNING);
        this.matricule   = matricule;
        this.departement = departement;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public String getMatricule()               { return matricule; }
    public void setMatricule(String m)         { this.matricule = m; }

    public String getDepartement()             { return departement; }
    public void setDepartement(String d)       { this.departement = d; }

    // ── equals / hashCode / toString ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResponsablePlanning r)) return false;
        return super.equals(o) && Objects.equals(matricule, r.matricule);
    }

    @Override
    public int hashCode() { return Objects.hash(super.hashCode(), matricule); }

    @Override
    public String toString() {
        return "ResponsablePlanning{id=" + getId() +
               ", matricule='" + matricule + "', departement='" + departement + "'}";
    }
}
