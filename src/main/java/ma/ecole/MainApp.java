package ma.ecole;

import config.DatabaseConnection;
import config.ServiceLocator;
import controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application JavaFX.
 * Assemble les dépendances via ServiceLocator, puis ouvre l'écran de login.
 */
public class MainApp extends Application {

    private static ServiceLocator serviceLocator;

    @Override
    public void start(Stage primaryStage) throws Exception {
        serviceLocator = new ServiceLocator();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/LoginView.fxml"));
        Parent root = loader.load();

        LoginController ctrl = loader.getController();
        ctrl.setAuthService(serviceLocator.getAuthService());

        primaryStage.setTitle("Gestion des Notes — Connexion");
        primaryStage.setScene(new Scene(root, 400, 320));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Fermeture propre du pool HikariCP
        DatabaseConnection.shutdown();
    }

    /** Permet aux controllers d'accéder au ServiceLocator global. */
    public static ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
