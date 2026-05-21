package model;

import java.util.Objects;

/** Enseignant — saisit et importe les notes de ses sous-modules. */
public class Enseignant extends Utilisateur {

    public enum Grade { ASSISTANT, MAITRE_ASSISTANT, PROFESSEUR }

    private String matricule;
    private String specialite;
    private Grade  grade;

    // ── Constructeurs ────────────────────────────────────────────────────────
    public Enseignant() { super(); }

    public Enseignant(String login, String motDePasse,
                      String nom, String prenom, String email,
                      String matricule, String specialite, Grade grade) {
        super(login, motDePasse, nom, prenom, email, Role.ENSEIGNANT);
        this.matricule  = matricule;
        this.specialite = specialite;
        this.grade      = grade;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public String getMatricule()               { return matricule; }
    public void setMatricule(String m)         { this.matricule = m; }

    public String getSpecialite()              { return specialite; }
    public void setSpecialite(String s)        { this.specialite = s; }

    public Grade getGrade()                    { return grade; }
    public void setGrade(Grade grade)          { this.grade = grade; }

    // ── equals / hashCode / toString ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Enseignant e)) return false;
        return super.equals(o) && Objects.equals(matricule, e.matricule);
    }

    @Override
    public int hashCode() { return Objects.hash(super.hashCode(), matricule); }

    @Override
    public String toString() {
        return "Enseignant{id=" + getId() + ", matricule='" + matricule +
               "', grade=" + grade + "}";
    }
}
