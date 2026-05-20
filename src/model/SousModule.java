package model;

import java.util.Objects;

/** Sous-module rattaché à un module, assigné à un enseignant. */
public class SousModule {

    private Long   id;
    private String code;
    private String intitule;
    private double coefficient;
    private Long   moduleId;
    private Long   enseignantId;       // nullable — peut être non assigné
    private String enseignantNom;      // dénormalisation pour affichage

    // ── Constructeurs ────────────────────────────────────────────────────────
    public SousModule() {}

    public SousModule(String code, String intitule, double coefficient,
                      Long moduleId, Long enseignantId) {
        this.code         = code;
        this.intitule     = intitule;
        this.coefficient  = coefficient;
        this.moduleId     = moduleId;
        this.enseignantId = enseignantId;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId()                            { return id; }
    public void setId(Long id)                     { this.id = id; }

    public String getCode()                        { return code; }
    public void setCode(String code)               { this.code = code; }

    public String getIntitule()                    { return intitule; }
    public void setIntitule(String intitule)       { this.intitule = intitule; }

    public double getCoefficient()                 { return coefficient; }
    public void setCoefficient(double c)           { this.coefficient = c; }

    public Long getModuleId()                      { return moduleId; }
    public void setModuleId(Long moduleId)         { this.moduleId = moduleId; }

    public Long getEnseignantId()                  { return enseignantId; }
    public void setEnseignantId(Long eid)          { this.enseignantId = eid; }

    public String getEnseignantNom()               { return enseignantNom; }
    public void setEnseignantNom(String n)         { this.enseignantNom = n; }

    // ── equals / hashCode / toString ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SousModule s)) return false;
        return Objects.equals(id, s.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "SousModule{id=" + id + ", code='" + code +
               "', intitule='" + intitule + "', coeff=" + coefficient +
               ", moduleId=" + moduleId + ", enseignantId=" + enseignantId + "}";
    }
}
