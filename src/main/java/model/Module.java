package model;

import java.util.Objects;

/** Module pédagogique rattaché à une promotion. */
public class Module {

    private Long   id;
    private String code;
    private String intitule;
    private double coefficient;
    private Long   promotionId;

    // ── Constructeurs ────────────────────────────────────────────────────────
    public Module() {}

    public Module(String code, String intitule, double coefficient, Long promotionId) {
        this.code        = code;
        this.intitule    = intitule;
        this.coefficient = coefficient;
        this.promotionId = promotionId;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getCode()                      { return code; }
    public void setCode(String code)             { this.code = code; }

    public String getIntitule()                  { return intitule; }
    public void setIntitule(String intitule)     { this.intitule = intitule; }

    public double getCoefficient()               { return coefficient; }
    public void setCoefficient(double c)         { this.coefficient = c; }

    public Long getPromotionId()                 { return promotionId; }
    public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }

    // ── equals / hashCode / toString ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Module m)) return false;
        return Objects.equals(id, m.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Module{id=" + id + ", code='" + code +
               "', intitule='" + intitule + "', coeff=" + coefficient + "}";
    }
}
