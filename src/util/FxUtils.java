package util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Button;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utilitaire JavaFX partagé par tous les Controllers.
 * Centralise : Task background, alertes, confirmations.
 */
public final class FxUtils {

    private FxUtils() {}

    /**
     * Lance un travail en background thread.
     * @param travail   lambda exécuté hors UI thread (appel service)
     * @param onSucces  lambda UI appelé avec le résultat (Platform.runLater automatique)
     * @param onEchec   lambda UI appelé avec l'exception
     * @param boutons   boutons à désactiver pendant le chargement
     */
    public static <T> void lancerTask(
            Supplier<T> travail,
            Consumer<T> onSucces,
            Consumer<Throwable> onEchec,
            Button... boutons) {

        Task<T> task = new Task<>() {
            @Override protected T call() { return travail.get(); }
        };

        task.setOnSucceeded(e -> {
            remettreBoutons(boutons);
            onSucces.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            remettreBoutons(boutons);
            onEchec.accept(task.getException());
        });

        desactiverBoutons(boutons);
        new Thread(task, "gestion-ecole-task").start();
    }

    /** Alerte erreur standardisée. */
    public static void erreur(String titre, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(titre);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }

    /** Alerte succès. */
    public static void succes(String titre, String message) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle(titre);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }

    /** Boîte de confirmation — retourne true si l'utilisateur confirme. */
    public static boolean confirmer(String titre, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(message);
        Optional<ButtonType> result = a.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void desactiverBoutons(Button... boutons) {
        for (Button b : boutons) if (b != null) b.setDisable(true);
    }

    private static void remettreBoutons(Button... boutons) {
        Platform.runLater(() -> {
            for (Button b : boutons) if (b != null) b.setDisable(false);
        });
    }
}
