package model;

import java.util.Objects;

/** Responsable filière — accès restreint à sa propre filière. */
public class ResponsableFiliere extends Utilisateur {

    private String matricule;
    private Long   filiereId;   // restriction d'accès gérée dans le Service

    // ── Constructeurs ────────────────────────────────────────────────────────
    public ResponsableFiliere() { super(); }

    public ResponsableFiliere(String login, String motDePasse,
                              String nom, String prenom, String email,
                              String matricule, Long filiereId) {
        super(login, motDePasse, nom, prenom, email, Role.RESPONSABLE_FILIERE);
        this.matricule = matricule;
        this.filiereId = filiereId;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public String getMatricule()               { return matricule; }
    public void setMatricule(String m)         { this.matricule = m; }

    public Long getFiliereId()                 { return filiereId; }
    public void setFiliereId(Long filiereId)   { this.filiereId = filiereId; }

    // ── equals / hashCode / toString ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResponsableFiliere r)) return false;
        return super.equals(o) && Objects.equals(matricule, r.matricule);
    }

    @Override
    public int hashCode() { return Objects.hash(super.hashCode(), matricule); }

    @Override
    public String toString() {
        return "ResponsableFiliere{id=" + getId() +
               ", matricule='" + matricule + "', filiereId=" + filiereId + "}";
    }
}
