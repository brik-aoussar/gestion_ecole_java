package controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Promotion;
import util.FxUtils;
import util.PDFExporter;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Dialog affichage rapport de promotion.
 */
public class RapportDialogCtrl {

    @FXML private TableView<Map<String, Object>>           tableRapport;
    @FXML private TableColumn<Map<String, Object>, String> colRang;
    @FXML private TableColumn<Map<String, Object>, String> colEtudiant;
    @FXML private TableColumn<Map<String, Object>, String> colMoyenne;
    @FXML private TableColumn<Map<String, Object>, String> colMention;
    @FXML private TableColumn<Map<String, Object>, String> colStatut;
    @FXML private Label                                    lblResume;
    @FXML private Button                                   btnExportPDF;

    private List<Map<String, Object>> rapport;
    private Promotion                 promotion;

    public void setRapport(List<Map<String, Object>> rapport) {
        this.rapport = rapport;
        afficherRapport();
    }

    public void setPromotion(Promotion p) { this.promotion = p; }

    @FXML
    public void initialize() {
        colRang.setCellValueFactory(c      -> new SimpleStringProperty(str(c.getValue(), "rang")));
        colEtudiant.setCellValueFactory(c  -> new SimpleStringProperty(str(c.getValue(), "etudiant")));
        colMoyenne.setCellValueFactory(c   -> new SimpleStringProperty(str(c.getValue(), "moyenne_generale")));
        colMention.setCellValueFactory(c   -> new SimpleStringProperty(str(c.getValue(), "mention")));
        colStatut.setCellValueFactory(c    -> new SimpleStringProperty(str(c.getValue(), "statut")));
    }

    private void afficherRapport() {
        if (rapport == null || rapport.isEmpty()) return;

        // Ligne 0 = résumé global
        Map<String, Object> resume = rapport.get(0);
        if ("RESUME".equals(resume.get("type"))) {
            lblResume.setText(
                "Total : " + resume.get("total_etudiants") +
                " | Taux réussite : " + resume.get("taux_reussite") +
                " | Meilleure moy : " + resume.get("meilleure_moy"));
            tableRapport.setItems(FXCollections.observableArrayList(rapport.subList(1, rapport.size())));
        } else {
            tableRapport.setItems(FXCollections.observableArrayList(rapport));
        }
    }

    @FXML
    public void handleExportPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("rapport_promotion.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File dest = fc.showSaveDialog(btnExportPDF.getScene().getWindow());
        if (dest == null) return;

        FxUtils.lancerTask(
            () -> { PDFExporter.exporterRapportPromotion(rapport, promotion, dest); return dest.getAbsolutePath(); },
            chemin -> FxUtils.succes("Export PDF", "Fichier enregistré :\n" + chemin),
            ex     -> FxUtils.erreur("Export PDF", ex.getMessage()),
            btnExportPDF
        );
    }

    @FXML public void handleFermer() { ((Stage) tableRapport.getScene().getWindow()).close(); }

    private String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v != null ? v.toString() : "";
    }
}
