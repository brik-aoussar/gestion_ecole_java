package controller;

import exception.ServiceException;
import exception.ValidationException;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Etudiant;
import model.Module;
import model.Promotion;
import service.EtudiantService;
import service.ModuleService;
import service.StatistiqueService;
import util.FxUtils;
import util.PDFExporter;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Controller principal du Responsable Planning & Évaluation.
 * Accès complet : étudiants, modules, rapports.
 */
public class ResponsableCtrl {

    // ── FXML — Onglet Étudiants ───────────────────────────────────────────────
    @FXML private TableView<Etudiant>             tableEtudiants;
    @FXML private TableColumn<Etudiant, String>   colCne;
    @FXML private TableColumn<Etudiant, String>   colNom;
    @FXML private TableColumn<Etudiant, String>   colPrenom;
    @FXML private TableColumn<Etudiant, String>   colStatut;
    @FXML private TextField                       txtRecherche;
    @FXML private ComboBox<Promotion>             cbPromotion;
    @FXML private Button                          btnAjouter;
    @FXML private Button                          btnModifier;
    @FXML private Button                          btnArchiver;

    // ── FXML — Onglet Modules ─────────────────────────────────────────────────
    @FXML private TableView<Module>               tableModules;
    @FXML private TableColumn<Module, String>     colCodeModule;
    @FXML private TableColumn<Module, String>     colIntituleModule;
    @FXML private TableColumn<Module, Double>     colCoeffModule;
    @FXML private Button                          btnAjouterModule;
    @FXML private Button                          btnSupprimerModule;

    // ── FXML — Barre commune ──────────────────────────────────────────────────
    @FXML private Label                           lblStatut;
    @FXML private Button                          btnRapportPDF;

    // ── Services ──────────────────────────────────────────────────────────────
    private EtudiantService    etudiantService;
    private ModuleService      moduleService;
    private StatistiqueService statistiqueService;

    /** Injection via ServiceLocator (appelé depuis MainApp ou LoginController). */
    public void setServices(EtudiantService es, ModuleService ms, StatistiqueService ss) {
        this.etudiantService    = es;
        this.moduleService      = ms;
        this.statistiqueService = ss;
    }

    // ── INITIALISATION ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurerColonnesEtudiants();
        configurerColonnesModules();
        configurerRechercheDynamique();
        // Les données sont chargées après injection des services (voir setServices)
    }

    /** Appelé après setServices() pour charger les données initiales. */
    public void chargerDonnees() {
        chargerEtudiants();
        chargerModules();
    }

    // ── ÉTUDIANTS ─────────────────────────────────────────────────────────────

    @FXML
    private void chargerEtudiants() {
        FxUtils.lancerTask(
            () -> etudiantService.recupererTous(),
            etudiants -> tableEtudiants.setItems(FXCollections.observableArrayList(etudiants)),
            ex -> FxUtils.erreur("Chargement", "Impossible de charger les étudiants : " + ex.getMessage()),
            btnAjouter
        );
    }

    @FXML
    public void handleAjouterEtudiant() {
        ouvrirDialogEtudiant(null);
    }

    @FXML
    public void handleModifierEtudiant() {
        Etudiant sel = tableEtudiants.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherStatut("⚠ Sélectionnez un étudiant"); return; }
        ouvrirDialogEtudiant(sel);
    }

    @FXML
    public void handleArchiverEtudiant() {
        Etudiant sel = tableEtudiants.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherStatut("⚠ Sélectionnez un étudiant"); return; }

        boolean confirme = FxUtils.confirmer("Archiver étudiant",
                "Archiver " + sel.getNomComplet() + " ?\nL'historique sera conservé.");
        if (!confirme) return;

        FxUtils.lancerTask(
            () -> { etudiantService.archiver(sel.getId()); return null; },
            r  -> { chargerEtudiants(); afficherStatut("✔ Étudiant archivé"); },
            ex -> FxUtils.erreur("Archivage", extraireMessage(ex)),
            btnArchiver
        );
    }

    @FXML
    public void handleRechercherEtudiant() {
        String terme = txtRecherche.getText().trim();
        if (terme.isEmpty()) { chargerEtudiants(); return; }

        FxUtils.lancerTask(
            () -> etudiantService.rechercher(terme),
            etudiants -> tableEtudiants.setItems(FXCollections.observableArrayList(etudiants)),
            ex -> FxUtils.erreur("Recherche", extraireMessage(ex))
        );
    }

    @FXML
    public void handleImporterExcel() {
        Promotion promo = cbPromotion.getValue();
        if (promo == null) { afficherStatut("⚠ Sélectionnez une promotion"); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Importer liste étudiants");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx", "*.xls"));
        File fichier = fc.showOpenDialog(btnAjouter.getScene().getWindow());
        if (fichier == null) return;

        FxUtils.lancerTask(
            () -> etudiantService.importerDepuisExcel(fichier, promo.getId()),
            nb  -> { chargerEtudiants(); afficherStatut("✔ " + nb + " étudiants importés"); },
            ex  -> FxUtils.erreur("Import Excel", extraireMessage(ex)),
            btnAjouter
        );
    }

    // ── MODULES ───────────────────────────────────────────────────────────────

    private void chargerModules() {
        Promotion promo = cbPromotion.getValue();
        if (promo == null) return;

        FxUtils.lancerTask(
            () -> moduleService.recupererParPromotion(promo.getId()),
            modules -> tableModules.setItems(FXCollections.observableArrayList(modules)),
            ex -> FxUtils.erreur("Chargement", "Erreur chargement modules : " + ex.getMessage()),
            btnAjouterModule
        );
    }

    @FXML
    public void handleAjouterModule() {
        ouvrirDialogModule(null);
    }

    @FXML
    public void handleSupprimerModule() {
        Module sel = tableModules.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherStatut("⚠ Sélectionnez un module"); return; }

        if (!FxUtils.confirmer("Supprimer module",
                "Supprimer le module \"" + sel.getIntitule() + "\" ?")) return;

        FxUtils.lancerTask(
            () -> { moduleService.supprimerModule(sel.getId()); return null; },
            r  -> { chargerModules(); afficherStatut("✔ Module supprimé"); },
            ex -> FxUtils.erreur("Suppression", extraireMessage(ex)),
            btnSupprimerModule
        );
    }

    // ── RAPPORT PDF ───────────────────────────────────────────────────────────

    @FXML
    public void handleGenererRapportPDF() {
        Promotion promo = cbPromotion.getValue();
        if (promo == null) { afficherStatut("⚠ Sélectionnez une promotion"); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport");
        fc.setInitialFileName("rapport_" + promo.getIntitule() + "_" + promo.getAnnee() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File dest = fc.showSaveDialog(btnRapportPDF.getScene().getWindow());
        if (dest == null) return;

        FxUtils.lancerTask(
            () -> {
                List<java.util.Map<String, Object>> rapport =
                        statistiqueService.getRapportParPromotion(promo.getId());
                PDFExporter.exporterRapportPromotion(rapport, promo, dest);
                return dest.getAbsolutePath();
            },
            chemin -> FxUtils.succes("Rapport généré", "Fichier enregistré :\n" + chemin),
            ex     -> FxUtils.erreur("Export PDF", extraireMessage(ex)),
            btnRapportPDF
        );
    }

    // ── CONFIGURATION COLONNES ────────────────────────────────────────────────

    private void configurerColonnesEtudiants() {
        colCne.setCellValueFactory(new PropertyValueFactory<>("cne"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colStatut.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatut().name()));
        tableEtudiants.setPlaceholder(new Label("Aucun étudiant"));
    }

    private void configurerColonnesModules() {
        colCodeModule.setCellValueFactory(new PropertyValueFactory<>("code"));
        colIntituleModule.setCellValueFactory(new PropertyValueFactory<>("intitule"));
        colCoeffModule.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getCoefficient()).asObject());
        tableModules.setPlaceholder(new Label("Aucun module"));
    }

    private void configurerRechercheDynamique() {
        if (txtRecherche != null)
            txtRecherche.textProperty().addListener((obs, old, nv) -> {
                if (nv.isBlank()) chargerEtudiants();
            });
    }

    // ── DIALOGS ───────────────────────────────────────────────────────────────

    private void ouvrirDialogEtudiant(Etudiant etudiant) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/dialog/EtudiantDialog.fxml"));
            Parent root = loader.load();
            EtudiantDialogCtrl ctrl = loader.getController();
            ctrl.setServices(etudiantService);
            ctrl.setEtudiant(etudiant);         // null = mode ajout
            ctrl.setOnSucces(() -> chargerEtudiants());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(etudiant == null ? "Ajouter un étudiant" : "Modifier étudiant");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException ex) {
            FxUtils.erreur("Erreur", "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    private void ouvrirDialogModule(Module module) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/dialog/ModuleDialog.fxml"));
            Parent root = loader.load();
            ModuleDialogCtrl ctrl = loader.getController();
            ctrl.setServices(moduleService);
            ctrl.setModule(module);
            ctrl.setPromotion(cbPromotion.getValue());
            ctrl.setOnSucces(() -> chargerModules());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(module == null ? "Ajouter un module" : "Modifier module");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException ex) {
            FxUtils.erreur("Erreur", "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void afficherStatut(String msg) {
        if (lblStatut != null) lblStatut.setText(msg);
    }

    private String extraireMessage(Throwable ex) {
        if (ex instanceof ValidationException || ex instanceof ServiceException)
            return ex.getMessage();
        return "Erreur inattendue : " + ex.getMessage();
    }
}
