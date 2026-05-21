package controller;

import exception.ServiceException;
import exception.ValidationException;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Module;
import model.Promotion;
import service.ModuleService;
import util.FxUtils;

public class ModuleDialogCtrl {

    @FXML private TextField txtCode;
    @FXML private TextField txtIntitule;
    @FXML private TextField txtCoefficient;
    @FXML private Button    btnSauvegarder;
    @FXML private Button    btnAnnuler;
    @FXML private Label     lblErreur;

    private ModuleService moduleService;
    private Module        moduleEnEdition;
    private Promotion     promotion;
    private Runnable      onSucces;

    public void setServices(ModuleService ms) { this.moduleService = ms; }
    public void setModule(Module m)           { this.moduleEnEdition = m; preFill(); }
    public void setPromotion(Promotion p)     { this.promotion = p; }
    public void setOnSucces(Runnable r)       { this.onSucces = r; }

    @FXML public void initialize() {
        lblErreur.setVisible(false);
        btnAnnuler.setOnAction(e -> fermer());
    }

    private void preFill() {
        if (moduleEnEdition == null) return;
        txtCode.setText(moduleEnEdition.getCode());
        txtIntitule.setText(moduleEnEdition.getIntitule());
        txtCoefficient.setText(String.valueOf(moduleEnEdition.getCoefficient()));
    }

    @FXML public void handleSauvegarder() {
        lblErreur.setVisible(false);
        try {
            double coeff = Double.parseDouble(txtCoefficient.getText().trim());
            Module m = moduleEnEdition != null ? moduleEnEdition : new Module();
            m.setCode(txtCode.getText().trim());
            m.setIntitule(txtIntitule.getText().trim());
            m.setCoefficient(coeff);
            if (promotion != null) m.setPromotionId(promotion.getId());

            FxUtils.lancerTask(
                () -> moduleEnEdition == null
                        ? moduleService.ajouterModule(m)
                        : moduleService.modifierModule(m),
                r -> { if (onSucces != null) onSucces.run(); fermer(); },
                ex -> { lblErreur.setText(ex.getMessage()); lblErreur.setVisible(true); },
                btnSauvegarder
            );
        } catch (NumberFormatException ex) {
            lblErreur.setText("[coefficient] Valeur numérique requise");
            lblErreur.setVisible(true);
        }
    }

    private void fermer() { ((Stage) btnSauvegarder.getScene().getWindow()).close(); }
}
