package controller;

import exception.ServiceException;
import exception.ValidationException;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Etudiant;
import service.EtudiantService;
import util.FxUtils;

import java.time.LocalDate;

/**
 * Dialog modal — Ajout / Modification étudiant.
 */
public class EtudiantDialogCtrl {

    @FXML private TextField  txtCne;
    @FXML private TextField  txtNom;
    @FXML private TextField  txtPrenom;
    @FXML private TextField  txtEmail;
    @FXML private TextField  txtTelephone;
    @FXML private DatePicker dpDateNaissance;
    @FXML private Button     btnSauvegarder;
    @FXML private Button     btnAnnuler;
    @FXML private Label      lblErreur;

    private EtudiantService etudiantService;
    private Etudiant        etudiantEnEdition; // null = mode ajout
    private Runnable        onSucces;

    public void setServices(EtudiantService es)  { this.etudiantService = es; }
    public void setEtudiant(Etudiant e)          { this.etudiantEnEdition = e; preFill(); }
    public void setOnSucces(Runnable r)          { this.onSucces = r; }

    @FXML
    public void initialize() {
        lblErreur.setVisible(false);
        btnAnnuler.setOnAction(e -> fermer());
    }

    private void preFill() {
        if (etudiantEnEdition == null) return;
        txtCne.setText(etudiantEnEdition.getCne());
        txtNom.setText(etudiantEnEdition.getNom());
        txtPrenom.setText(etudiantEnEdition.getPrenom());
        txtEmail.setText(etudiantEnEdition.getEmail());
        txtTelephone.setText(etudiantEnEdition.getTelephone());
        dpDateNaissance.setValue(etudiantEnEdition.getDateNaissance());
        txtCne.setDisable(true); // CNE non modifiable
    }

    @FXML
    public void handleSauvegarder() {
        lblErreur.setVisible(false);

        Etudiant e = etudiantEnEdition != null ? etudiantEnEdition : new Etudiant();
        e.setCne(txtCne.getText().trim());
        e.setNom(txtNom.getText().trim());
        e.setPrenom(txtPrenom.getText().trim());
        e.setEmail(txtEmail.getText().trim());
        e.setTelephone(txtTelephone.getText().trim());
        e.setDateNaissance(dpDateNaissance.getValue());

        FxUtils.lancerTask(
            () -> {
                if (etudiantEnEdition == null) return etudiantService.ajouter(e);
                else return etudiantService.modifier(e);
            },
            result -> {
                if (onSucces != null) onSucces.run();
                fermer();
            },
            ex -> {
                String msg = (ex instanceof ValidationException || ex instanceof ServiceException)
                        ? ex.getMessage() : "Erreur inattendue";
                lblErreur.setText(msg);
                lblErreur.setVisible(true);
            },
            btnSauvegarder
        );
    }

    private void fermer() {
        ((Stage) btnSauvegarder.getScene().getWindow()).close();
    }
}
