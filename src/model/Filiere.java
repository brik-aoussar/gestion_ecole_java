package model;

import java.util.Objects;

/** Filière de formation. */
public class Filiere {

    private Long   id;
    private String code;
    private String intitule;
    private String domaine;

    // ── Constructeurs ────────────────────────────────────────────────────────
    public Filiere() {}

    public Filiere(String code, String intitule, String domaine) {
        this.code     = code;
        this.intitule = intitule;
        this.domaine  = domaine;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getCode()                    { return code; }
    public void setCode(String code)           { this.code = code; }

    public String getIntitule()                { return intitule; }
    public void setIntitule(String intitule)   { this.intitule = intitule; }

    public String getDomaine()                 { return domaine; }
    public void setDomaine(String domaine)     { this.domaine = domaine; }

    // ── equals / hashCode / toString ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Filiere f)) return false;
        return Objects.equals(id, f.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Filiere{id=" + id + ", code='" + code + "', intitule='" + intitule + "'}";
    }
}
