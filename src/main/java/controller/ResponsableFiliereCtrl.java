package controller;

import exception.ServiceException;
import exception.ValidationException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Etudiant;
import model.ResponsableFiliere;
import model.Utilisateur;
import service.EtudiantService;
import service.NoteService;
import service.StatistiqueService;
import util.FxUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller Responsable Filière — accès RESTREINT à sa propre filière.
 * La restriction est appliquée ici ET dans le service.
 */
public class ResponsableFiliereCtrl {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private TableView<Etudiant>           tableEtudiants;
    @FXML private TableColumn<Etudiant, String> colCne;
    @FXML private TableColumn<Etudiant, String> colNom;
    @FXML private TableColumn<Etudiant, String> colPrenom;
    @FXML private TableColumn<Etudiant, String> colStatut;
    @FXML private Label                         lblFiliere;
    @FXML private Label                         lblStatut;
    @FXML private Button                        btnValiderNotes;
    @FXML private Button                        btnVoirResultats;
    @FXML private ComboBox<Long>                cbPromotion;   // promotions de la filière

    // ── État ──────────────────────────────────────────────────────────────────
    private Utilisateur utilisateurConnecte;
    private Long        filiereId;

    // ── Services ──────────────────────────────────────────────────────────────
    private EtudiantService    etudiantService;
    private NoteService        noteService;
    private StatistiqueService statistiqueService;

    public void setServices(EtudiantService es, NoteService ns, StatistiqueService ss) {
        this.etudiantService    = es;
        this.noteService        = ns;
        this.statistiqueService = ss;
    }

    /** Reçoit l'utilisateur connecté depuis LoginController. */
    public void setUtilisateurConnecte(Utilisateur u) {
        this.utilisateurConnecte = u;
        if (u instanceof ResponsableFiliere rf) {
            this.filiereId = rf.getFiliereId();
        } else {
            // Sécurité : si le type est inattendu, on bloque tout
            FxUtils.erreur("Accès refusé", "Ce compte ne possède pas d'accès filière.");
            filiereId = null;
        }
    }

    @FXML
    public void initialize() {
        configurerColonnes();
    }

    /** Appelé après setUtilisateurConnecte + setServices. */
    public void chargerDonnees() {
        if (filiereId == null) return;
        afficherInfoFiliere();
        chargerEtudiants();
    }

    // ── CHARGER ÉTUDIANTS (filière restreinte) ────────────────────────────────
    private void chargerEtudiants() {
        FxUtils.lancerTask(
            // Service applique le filtre filiereId — on ne peut pas voir autre chose
            () -> etudiantService.recupererParFiliere(filiereId),
            etudiants -> {
                tableEtudiants.setItems(FXCollections.observableArrayList(etudiants));
                afficherStatut("✔ " + etudiants.size() + " étudiant(s) chargé(s)");
            },
            ex -> FxUtils.erreur("Chargement", extraireMessage(ex)),
            btnValiderNotes
        );
    }

    // ── VALIDER NOTES ─────────────────────────────────────────────────────────
    @FXML
    public void handleValiderNotes() {
        if (filiereId == null) return;
        Long promotionId = cbPromotion.getValue();
        if (promotionId == null) {
            afficherStatut("⚠ Sélectionnez une promotion");
            return;
        }

        boolean confirme = FxUtils.confirmer("Valider les notes",
                "Confirmer la validation de toutes les notes pour cette promotion ?\n" +
                "Cette action est définitive.");
        if (!confirme) return;

        FxUtils.lancerTask(
            () -> {
                // Valide tous les sous-modules de la promotion
                // Le service vérifie que toutes les notes sont saisies avant de valider
                noteService.validerNotesSousModule(promotionId);
                return null;
            },
            r  -> { afficherStatut("✔ Notes validées avec succès"); },
            ex -> FxUtils.erreur("Validation", extraireMessage(ex)),
            btnValiderNotes
        );
    }

    // ── CONSULTER RÉSULTATS ───────────────────────────────────────────────────
    @FXML
    public void handleConsulterResultats() {
        if (filiereId == null) return;
        Long promotionId = cbPromotion.getValue();
        if (promotionId == null) {
            afficherStatut("⚠ Sélectionnez une promotion");
            return;
        }

        FxUtils.lancerTask(
            () -> statistiqueService.getRapportParPromotion(promotionId),
            rapport -> afficherRapportDialog(rapport, promotionId),
            ex     -> FxUtils.erreur("Résultats", extraireMessage(ex)),
            btnVoirResultats
        );
    }

    // ── DIALOG RAPPORT ────────────────────────────────────────────────────────
    private void afficherRapportDialog(List<Map<String, Object>> rapport, Long promotionId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/dialog/RapportDialog.fxml"));
            Parent root = loader.load();
            RapportDialogCtrl ctrl = loader.getController();
            ctrl.setRapport(rapport);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Résultats — Promotion " + promotionId);
            dialog.setScene(new Scene(root, 800, 600));
            dialog.showAndWait();
        } catch (IOException ex) {
            FxUtils.erreur("Erreur", "Impossible d'afficher le rapport : " + ex.getMessage());
        }
    }

    // ── CONFIGURATION COLONNES ────────────────────────────────────────────────
    private void configurerColonnes() {
        colCne.setCellValueFactory(new PropertyValueFactory<>("cne"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colStatut.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatut().name()));
        tableEtudiants.setPlaceholder(new Label("Aucun étudiant dans cette filière"));
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private void afficherInfoFiliere() {
        if (lblFiliere != null && utilisateurConnecte != null)
            lblFiliere.setText("Filière : " + utilisateurConnecte.getNomComplet()
                    + " | ID Filière : " + filiereId);
    }

    private void afficherStatut(String msg) {
        if (lblStatut != null) lblStatut.setText(msg);
    }

    private String extraireMessage(Throwable ex) {
        if (ex instanceof ValidationException || ex instanceof ServiceException)
            return ex.getMessage();
        return "Erreur inattendue : " + ex.getMessage();
    }
}
