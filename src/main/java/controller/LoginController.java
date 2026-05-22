package controller;

import config.ServiceLocator;
import exception.AuthException;
import exception.ValidationException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Utilisateur;
import service.AuthService;
import service.impl.AuthServiceImpl;

import java.io.IOException;

public class LoginController {

    @FXML private TextField         txtLogin;
    @FXML private PasswordField     txtPassword;
    @FXML private Label             lblErreur;
    @FXML private Button            btnConnexion;
    @FXML private ProgressIndicator progress;

    private AuthService authService;

    @FXML
    public void initialize() {
        lblErreur.setVisible(false);
        progress.setVisible(false);
        txtPassword.setOnAction(e -> handleLogin());
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    public void handleLogin() {
        String login    = txtLogin.getText().trim();
        String password = txtPassword.getText();

        if (login.isEmpty() || password.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs");
            return;
        }

        lblErreur.setVisible(false);
        btnConnexion.setDisable(true);
        progress.setVisible(true);

        String passwordHash = AuthServiceImpl.sha256(password);

        Task<Utilisateur> task = new Task<>() {
            @Override
            protected Utilisateur call() {
                return authService.login(login, passwordHash);
            }
        };

        task.setOnSucceeded(e -> {
            btnConnexion.setDisable(false);
            progress.setVisible(false);
            routerVersDashboard(task.getValue());
        });

        task.setOnFailed(e -> {
            btnConnexion.setDisable(false);
            progress.setVisible(false);
            Throwable ex = task.getException();
            if (ex instanceof AuthException || ex instanceof ValidationException)
                afficherErreur(ex.getMessage());
            else
                afficherErreur("Erreur de connexion — verifiez votre reseau");
        });

        new Thread(task, "login-thread").start();
    }

    private void routerVersDashboard(Utilisateur u) {
        try {
            String fxml = switch (u.getRole()) {
                case RESPONSABLE_PLANNING -> "/view/ResponsableView.fxml";
                case RESPONSABLE_FILIERE  -> "/view/ResponsableFiliereView.fxml";
                case ENSEIGNANT           -> "/view/EnseignantView.fxml";
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            ServiceLocator sl = ma.ecole.MainApp.getServiceLocator();

            switch (u.getRole()) {
                case RESPONSABLE_PLANNING -> {
                    ResponsableCtrl ctrl = loader.getController();
                    ctrl.setServices(
                        sl.getEtudiantService(),
                        sl.getModuleService(),
                        sl.getStatistiqueService()
                    );
                    ctrl.chargerDonnees();
                }
                case RESPONSABLE_FILIERE -> {
                    ResponsableFiliereCtrl ctrl = loader.getController();
                    ctrl.setServices(
                        sl.getEtudiantService(),
                        sl.getNoteService(),
                        sl.getStatistiqueService()
                    );
                    ctrl.setUtilisateurConnecte(u);
                    ctrl.chargerDonnees();
                }
                case ENSEIGNANT -> {
                    EnseignantCtrl ctrl = loader.getController();
                    ctrl.setServices(
                        sl.getNoteService(),
                        sl.getModuleService(),
                        sl.getStatistiqueService()
                    );
                    ctrl.setUtilisateurConnecte(u);
                    ctrl.chargerDonnees();
                }
            }

            Stage stage = (Stage) btnConnexion.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Notes — " + u.getNomComplet());
            stage.centerOnScreen();

        } catch (Exception ex) {
            ex.printStackTrace();
            afficherErreur("Impossible d'ouvrir le tableau de bord : " + ex.getMessage());
        }
    }

    private void afficherErreur(String message) {
        lblErreur.setText(message);
        lblErreur.setVisible(true);
        txtPassword.clear();
        txtPassword.requestFocus();
    }
}
