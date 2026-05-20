package model;

import java.util.Objects;

/** Promotion = filière + année académique. */
public class Promotion {

    private Long    id;
    private String  intitule;
    private int     annee;
    private Long    filiereId;
    private String  filiereIntitule;   // dénormalisation pour affichage
    private boolean actif;

    // ── Constructeurs ────────────────────────────────────────────────────────
    public Promotion() {}

    public Promotion(String intitule, int annee, Long filiereId) {
        this.intitule  = intitule;
        this.annee     = annee;
        this.filiereId = filiereId;
        this.actif     = true;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public String getIntitule()                      { return intitule; }
    public void setIntitule(String intitule)         { this.intitule = intitule; }

    public int getAnnee()                            { return annee; }
    public void setAnnee(int annee)                  { this.annee = annee; }

    public Long getFiliereId()                       { return filiereId; }
    public void setFiliereId(Long filiereId)         { this.filiereId = filiereId; }

    public String getFiliereIntitule()               { return filiereIntitule; }
    public void setFiliereIntitule(String fi)        { this.filiereIntitule = fi; }

    public boolean isActif()                         { return actif; }
    public void setActif(boolean actif)              { this.actif = actif; }

    public String getLibelleComplet() {
        return intitule + " — " + annee +
               (filiereIntitule != null ? " (" + filiereIntitule + ")" : "");
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Promotion p)) return false;
        return Objects.equals(id, p.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Promotion{id=" + id + ", intitule='" + intitule +
               "', annee=" + annee + ", filiereId=" + filiereId + "}";
    }
}
