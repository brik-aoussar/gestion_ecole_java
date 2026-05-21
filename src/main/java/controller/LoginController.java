package controller;

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

/**
 * Controller — écran de connexion.
 * ZÉRO accès BDD. Tout passe par AuthService.
 */
public class LoginController {

    // ── Injections FXML ───────────────────────────────────────────────────────
    @FXML private TextField         txtLogin;
    @FXML private PasswordField     txtPassword;
    @FXML private Label             lblErreur;
    @FXML private Button            btnConnexion;
    @FXML private ProgressIndicator progress;

    // ── Dépendances ───────────────────────────────────────────────────────────
    private AuthService authService;

    @FXML
    public void initialize() {
        lblErreur.setVisible(false);
        progress.setVisible(false);
        // Connexion au Enter dans le champ password
        txtPassword.setOnAction(e -> handleLogin());
    }

    /** Injection du service depuis MainApp ou ServiceLocator. */
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    // ── HANDLE LOGIN ──────────────────────────────────────────────────────────
    @FXML
    public void handleLogin() {
        String login    = txtLogin.getText().trim();
        String password = txtPassword.getText();

        // Validation rapide côté UI (avant thread)
        if (login.isEmpty() || password.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs");
            return;
        }

        // Prépare l'UI pour le chargement
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
            Utilisateur u = task.getValue();
            routerVersDashboard(u);
        });

        task.setOnFailed(e -> {
            btnConnexion.setDisable(false);
            progress.setVisible(false);
            Throwable ex = task.getException();
            if (ex instanceof AuthException || ex instanceof ValidationException)
                afficherErreur(ex.getMessage());
            else
                afficherErreur("Erreur de connexion — vérifiez votre réseau");
        });

        new Thread(task, "login-thread").start();
    }

    // ── ROUTING PAR RÔLE ─────────────────────────────────────────────────────
    private void routerVersDashboard(Utilisateur u) {
        try {
            String fxml = switch (u.getRole()) {
                case RESPONSABLE_PLANNING -> "/view/ResponsableView.fxml";
                case RESPONSABLE_FILIERE  -> "/view/ResponsableFiliereView.fxml";
                case ENSEIGNANT           -> "/view/EnseignantView.fxml";
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            // Passe l'utilisateur connecté au controller cible
            switch (u.getRole()) {
                case RESPONSABLE_PLANNING -> {} // ResponsableCtrl n'a pas besoin de l'objet user
                case RESPONSABLE_FILIERE -> {
                    ResponsableFiliereCtrl ctrl = loader.getController();
                    ctrl.setUtilisateurConnecte(u);
                }
                case ENSEIGNANT -> {
                    EnseignantCtrl ctrl = loader.getController();
                    ctrl.setUtilisateurConnecte(u);
                }
            }

            Stage stage = (Stage) btnConnexion.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des Notes — " + u.getNomComplet());
            stage.centerOnScreen();

        } catch (IOException ex) {
            afficherErreur("Impossible d'ouvrir le tableau de bord : " + ex.getMessage());
        }
    }

    // ── UI HELPERS ────────────────────────────────────────────────────────────
    private void afficherErreur(String message) {
        lblErreur.setText(message);
        lblErreur.setVisible(true);
        txtPassword.clear();
        txtPassword.requestFocus();
    }
}
