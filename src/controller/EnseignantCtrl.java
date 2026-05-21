package controller;

import exception.ServiceException;
import exception.ValidationException;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;
import model.*;
import service.ModuleService;
import service.NoteService;
import service.StatistiqueService;
import util.FxUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller Enseignant — saisie et import des notes de ses sous-modules.
 * L'accès est limité aux sous-modules assignés à cet enseignant.
 */
public class EnseignantCtrl {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private ComboBox<SousModule>             cbSousModules;
    @FXML private TableView<LigneNote>             tableNotes;
    @FXML private TableColumn<LigneNote, String>   colEtudiant;
    @FXML private TableColumn<LigneNote, String>   colCne;
    @FXML private TableColumn<LigneNote, Double>   colNote;
    @FXML private TableColumn<LigneNote, String>   colType;
    @FXML private Label                            lblStatut;
    @FXML private Label                            lblMoyenne;
    @FXML private Button                           btnEnregistrer;
    @FXML private Button                           btnImporter;
    @FXML private ComboBox<Note.TypeNote>          cbTypeNote;

    // ── État ──────────────────────────────────────────────────────────────────
    private Utilisateur utilisateurConnecte;
    private Long        enseignantId;

    // ── Services ──────────────────────────────────────────────────────────────
    private NoteService        noteService;
    private ModuleService      moduleService;
    private StatistiqueService statistiqueService;

    public void setServices(NoteService ns, ModuleService ms, StatistiqueService ss) {
        this.noteService        = ns;
        this.moduleService      = ms;
        this.statistiqueService = ss;
    }

    public void setUtilisateurConnecte(Utilisateur u) {
        this.utilisateurConnecte = u;
        this.enseignantId        = u.getId();
    }

    @FXML
    public void initialize() {
        configurerTableNotes();
        cbTypeNote.setItems(FXCollections.observableArrayList(Note.TypeNote.values()));
        cbTypeNote.setValue(Note.TypeNote.EXAMEN);
    }

    public void chargerDonnees() {
        if (enseignantId == null) return;
        chargerSousModules();
    }

    // ── CHARGER SOUS-MODULES DE L'ENSEIGNANT ─────────────────────────────────
    private void chargerSousModules() {
        FxUtils.lancerTask(
            () -> moduleService.recupererParEnseignant(enseignantId),
            sousModules -> {
                cbSousModules.setItems(FXCollections.observableArrayList(sousModules));
                cbSousModules.setConverter(new javafx.util.StringConverter<>() {
                    public String toString(SousModule sm) {
                        return sm == null ? "" : sm.getCode() + " — " + sm.getIntitule();
                    }
                    public SousModule fromString(String s) { return null; }
                });
                if (!sousModules.isEmpty()) {
                    cbSousModules.setValue(sousModules.get(0));
                    handleSelectionSousModule();
                }
            },
            ex -> FxUtils.erreur("Chargement", extraireMessage(ex))
        );
    }

    // ── SÉLECTION SOUS-MODULE → charge les notes existantes ──────────────────
    @FXML
    public void handleSelectionSousModule() {
        SousModule sm = cbSousModules.getValue();
        if (sm == null) return;

        FxUtils.lancerTask(
            () -> noteService.getNotesParSousModule(sm.getId()),
            notes -> {
                ObservableList<LigneNote> lignes = FXCollections.observableArrayList();
                notes.forEach(n -> lignes.add(new LigneNote(
                        n.getId(), n.getEtudiantId(),
                        n.getEtudiantNom() != null ? n.getEtudiantNom() : "Étudiant #" + n.getEtudiantId(),
                        "",
                        n.getValeur(), n.getTypeNote())));
                tableNotes.setItems(lignes);
                calculerMoyenneAffichee(lignes);
                afficherStatut("✔ " + lignes.size() + " note(s) chargée(s)");
            },
            ex -> FxUtils.erreur("Chargement notes", extraireMessage(ex))
        );
    }

    // ── ENREGISTRER NOTES (saisie manuelle) ───────────────────────────────────
    @FXML
    public void handleSaisirNotes() {
        SousModule sm = cbSousModules.getValue();
        if (sm == null) { afficherStatut("⚠ Sélectionnez un sous-module"); return; }

        ObservableList<LigneNote> lignes = tableNotes.getItems();
        if (lignes.isEmpty()) { afficherStatut("⚠ Aucune note à enregistrer"); return; }

        // Vérifie les valeurs avant d'envoyer
        for (LigneNote l : lignes) {
            if (l.getNote() < 0 || l.getNote() > 20) {
                FxUtils.erreur("Note invalide",
                        "La note de " + l.getNomEtudiant() + " doit être entre 0 et 20.");
                return;
            }
        }

        Note.TypeNote type = cbTypeNote.getValue();

        FxUtils.lancerTask(
            () -> {
                int count = 0;
                for (LigneNote l : lignes) {
                    Note n = new Note(l.getNote(), type, l.getEtudiantId(),
                                      sm.getId(), enseignantId);
                    noteService.saisirNote(n);
                    count++;
                }
                return count;
            },
            nb -> {
                afficherStatut("✔ " + nb + " note(s) enregistrée(s)");
                calculerMoyenneAffichee(tableNotes.getItems());
            },
            ex -> FxUtils.erreur("Enregistrement", extraireMessage(ex)),
            btnEnregistrer
        );
    }

    // ── IMPORTER DEPUIS EXCEL ─────────────────────────────────────────────────
    @FXML
    public void handleImporterNotes() {
        SousModule sm = cbSousModules.getValue();
        if (sm == null) { afficherStatut("⚠ Sélectionnez un sous-module"); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Importer notes depuis Excel");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx", "*.xls"));
        File fichier = fc.showOpenDialog(btnImporter.getScene().getWindow());
        if (fichier == null) return;

        FxUtils.lancerTask(
            () -> noteService.importerNotes(fichier, sm.getId(), enseignantId),
            nb -> {
                afficherStatut("✔ " + nb + " note(s) importée(s)");
                handleSelectionSousModule(); // refresh tableau
            },
            ex -> FxUtils.erreur("Import Excel", extraireMessage(ex)),
            btnImporter
        );
    }

    // ── CONSULTER MOYENNES des étudiants affichés ─────────────────────────────
    @FXML
    public void handleConsulterMoyennes() {
        SousModule sm = cbSousModules.getValue();
        if (sm == null) return;

        // On cherche l'id promotion depuis le module parent
        FxUtils.lancerTask(
            () -> {
                // Moyenne pondérée de chaque étudiant du tableau
                List<String> resultats = new ArrayList<>();
                for (LigneNote l : tableNotes.getItems()) {
                    // promotionId fictif ici — à récupérer via moduleService si besoin
                    resultats.add(l.getNomEtudiant() + " → note : " + l.getNote());
                }
                return resultats;
            },
            resultats -> {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Récapitulatif notes");
                info.setHeaderText("Sous-module : " + sm.getIntitule());
                info.setContentText(String.join("\n", resultats));
                info.showAndWait();
            },
            ex -> FxUtils.erreur("Erreur", extraireMessage(ex))
        );
    }

    // ── CONFIGURATION TABLE NOTES ÉDITABLES ───────────────────────────────────
    private void configurerTableNotes() {
        colEtudiant.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNomEtudiant()));
        colCne.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCne()));
        colType.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTypeNote().name()));

        // Colonne note ÉDITABLE avec validation inline
        colNote.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getNote()).asObject());
        colNote.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        colNote.setOnEditCommit(event -> {
            double val = event.getNewValue();
            if (val < 0 || val > 20) {
                FxUtils.erreur("Note invalide", "La note doit être entre 0 et 20");
                tableNotes.refresh();
                return;
            }
            event.getRowValue().setNote(val);
            calculerMoyenneAffichee(tableNotes.getItems());
        });

        tableNotes.setEditable(true);
        tableNotes.setPlaceholder(new Label("Sélectionnez un sous-module pour afficher les notes"));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private void calculerMoyenneAffichee(ObservableList<LigneNote> lignes) {
        if (lignes.isEmpty()) { if (lblMoyenne != null) lblMoyenne.setText("Moy : —"); return; }
        double moy = lignes.stream().mapToDouble(LigneNote::getNote).average().orElse(0.0);
        if (lblMoyenne != null)
            lblMoyenne.setText(String.format("Moyenne classe : %.2f / 20", moy));
    }

    private void afficherStatut(String msg) {
        if (lblStatut != null) lblStatut.setText(msg);
    }

    private String extraireMessage(Throwable ex) {
        if (ex instanceof ValidationException || ex instanceof ServiceException)
            return ex.getMessage();
        return "Erreur inattendue : " + ex.getMessage();
    }

    // ── CLASSE INTERNE — modèle ligne tableau ────────────────────────────────
    public static class LigneNote {
        private final Long          noteId;
        private final Long          etudiantId;
        private final String        nomEtudiant;
        private final String        cne;
        private double              note;
        private final Note.TypeNote typeNote;

        public LigneNote(Long noteId, Long etudiantId, String nomEtudiant,
                         String cne, double note, Note.TypeNote typeNote) {
            this.noteId      = noteId;
            this.etudiantId  = etudiantId;
            this.nomEtudiant = nomEtudiant;
            this.cne         = cne;
            this.note        = note;
            this.typeNote    = typeNote;
        }

        public Long          getNoteId()      { return noteId; }
        public Long          getEtudiantId()  { return etudiantId; }
        public String        getNomEtudiant() { return nomEtudiant; }
        public String        getCne()         { return cne; }
        public double        getNote()        { return note; }
        public void          setNote(double n){ this.note = n; }
        public Note.TypeNote getTypeNote()    { return typeNote; }
    }
}
