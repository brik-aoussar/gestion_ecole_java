package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Charge et expose config.properties depuis le classpath.
 * Singleton thread-safe via initialisation statique.
 *
 * Utilisation :
 *   String url = AppConfig.get().getDatabaseUrl();
 */
public final class AppConfig {

    private static final String FICHIER_CONFIG = "/config.properties";
    private static final AppConfig INSTANCE = new AppConfig();

    private final Properties props = new Properties();

    // ── Chargement ────────────────────────────────────────────────────────────
    private AppConfig() {
        try (InputStream in = AppConfig.class.getResourceAsStream(FICHIER_CONFIG)) {
            if (in == null)
                throw new ExceptionInInitializerError(
                        "Fichier de configuration introuvable : " + FICHIER_CONFIG);
            props.load(in);
        } catch (IOException ex) {
            throw new ExceptionInInitializerError("Erreur lecture config.properties : " + ex.getMessage());
        }
        creerDossiersExport();
    }

    public static AppConfig get() { return INSTANCE; }

    // ── Base de données ───────────────────────────────────────────────────────
    public String getDatabaseUrl()      { return props.getProperty("db.url"); }
    public String getDatabaseUser()     { return props.getProperty("db.user"); }
    public String getDatabasePassword() { return props.getProperty("db.password", ""); }
    public int    getPoolMax()          { return intProp("db.pool.max", 10); }
    public int    getPoolMin()          { return intProp("db.pool.min", 2); }

    // ── Exports ───────────────────────────────────────────────────────────────
    public String getExportPath()    { return props.getProperty("export.path", "exports/"); }
    public String getLogoPath()      { return props.getProperty("export.pdf.logo", ""); }

    // ── Application ───────────────────────────────────────────────────────────
    public String getAppName()       { return props.getProperty("app.name", "Gestion Notes"); }
    public String getAppVersion()    { return props.getProperty("app.version", "1.0.0"); }
    public int    getPageSize()      { return intProp("app.page.size", Constantes.MAX_ETUDIANTS_PAR_PAGE); }

    // ── Accès générique ───────────────────────────────────────────────────────
    public String get(String cle)                   { return props.getProperty(cle); }
    public String get(String cle, String defaut)    { return props.getProperty(cle, defaut); }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private int intProp(String cle, int defaut) {
        try { return Integer.parseInt(props.getProperty(cle)); }
        catch (Exception ex) { return defaut; }
    }

    /** Crée les dossiers d'export au démarrage s'ils n'existent pas. */
    private void creerDossiersExport() {
        Path exportDir = Paths.get(getExportPath());
        if (!Files.exists(exportDir)) {
            try { Files.createDirectories(exportDir); }
            catch (IOException ex) {
                System.err.println("Avertissement : impossible de créer le dossier export : " + ex.getMessage());
            }
        }
    }
}
