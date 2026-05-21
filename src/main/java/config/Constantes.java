package config;

/**
 * Constantes globales de l'application.
 * Toute valeur "magique" doit être déclarée ici, jamais en dur dans le code.
 */
public final class Constantes {

    private Constantes() {}

    // ── Règles métier ─────────────────────────────────────────────────────────
    public static final double MOYENNE_REUSSITE         = 10.0;
    public static final double COEFF_ELIMINATOIRE       = 4.0;   // coeff module critique
    public static final double NOTE_MIN                 = 0.0;
    public static final double NOTE_MAX                 = 20.0;

    // ── Pagination ────────────────────────────────────────────────────────────
    public static final int    MAX_ETUDIANTS_PAR_PAGE   = 50;
    public static final int    MAX_RESULTATS_RECHERCHE  = 100;

    // ── Rôles (miroir de l'enum Utilisateur.Role pour usage FXML/String) ─────
    public static final String ROLE_RESPONSABLE_PLANNING = "RESPONSABLE_PLANNING";
    public static final String ROLE_RESPONSABLE_FILIERE  = "RESPONSABLE_FILIERE";
    public static final String ROLE_ENSEIGNANT           = "ENSEIGNANT";

    // ── Statuts étudiant ──────────────────────────────────────────────────────
    public static final String STATUT_ACTIF     = "ACTIF";
    public static final String STATUT_ARCHIVE   = "ARCHIVE";
    public static final String STATUT_SUSPENDU  = "SUSPENDU";

    // ── Types de note ─────────────────────────────────────────────────────────
    public static final String TYPE_EXAMEN           = "EXAMEN";
    public static final String TYPE_TP               = "TP";
    public static final String TYPE_CONTROLE_CONTINU = "CONTROLE_CONTINU";
    public static final String TYPE_PROJET           = "PROJET";

    // ── Mentions ──────────────────────────────────────────────────────────────
    public static final double MENTION_TRES_BIEN  = 16.0;
    public static final double MENTION_BIEN       = 14.0;
    public static final double MENTION_ASSEZ_BIEN = 12.0;
    public static final double MENTION_PASSABLE   = 10.0;

    // ── Formats ───────────────────────────────────────────────────────────────
    public static final String FORMAT_DATE        = "dd/MM/yyyy";
    public static final String FORMAT_DATETIME    = "dd/MM/yyyy HH:mm";
    public static final String REGEX_EMAIL        = "^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$";
    public static final String REGEX_CNE          = "^[A-Z]{1,2}[0-9]{6,10}$";

    // ── Fichiers export ───────────────────────────────────────────────────────
    public static final String EXT_PDF   = ".pdf";
    public static final String EXT_EXCEL = ".xlsx";

    // ── Messages utilisateur standards ────────────────────────────────────────
    public static final String MSG_CHAMP_OBLIGATOIRE  = "Ce champ est obligatoire";
    public static final String MSG_EMAIL_INVALIDE     = "Format d'email invalide";
    public static final String MSG_NOTE_INVALIDE      = "La note doit être entre 0 et 20";
    public static final String MSG_DATE_INVALIDE      = "La date doit être dans le passé";
    public static final String MSG_SUCCES_ENREGISTREMENT = "Enregistrement effectué avec succès";
    public static final String MSG_SUCCES_SUPPRESSION    = "Suppression effectuée avec succès";
    public static final String MSG_CONFIRMATION_ARCHIVAGE = "Confirmer l'archivage de cet étudiant ?";
    public static final String MSG_CONFIRMATION_SUPPRESSION = "Cette action est irréversible. Confirmer ?";
}
