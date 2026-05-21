package config;

import dao.*;
import service.*;
import service.impl.*;

/**
 * Assemblage manuel de toutes les dépendances (DAO → Service).
 * Instancier une seule fois au démarrage de l'application JavaFX.
 *
 * Utilisation dans MainApp :
 *   ServiceLocator sl = new ServiceLocator();
 *   EtudiantService es = sl.getEtudiantService();
 */
public class ServiceLocator {

    // ── DAO ───────────────────────────────────────────────────────────────────
    private final FiliereDAO      filiereDAO      = new FiliereDAO();
    private final PromotionDAO    promotionDAO    = new PromotionDAO();
    private final EtudiantDAO     etudiantDAO     = new EtudiantDAO();
    private final ModuleDAO       moduleDAO       = new ModuleDAO();
    private final NoteDAO         noteDAO         = new NoteDAO();
    private final UtilisateurDAO  utilisateurDAO  = new UtilisateurDAO();
    private final StatistiqueDAO  statistiqueDAO  = new StatistiqueDAO();

    // ── SERVICES ──────────────────────────────────────────────────────────────
    private final AuthService         authService;
    private final FiliereService      filiereService;
    private final EtudiantService     etudiantService;
    private final ModuleService       moduleService;
    private final NoteService         noteService;
    private final StatistiqueService  statistiqueService;

    public ServiceLocator() {
        this.authService        = new AuthServiceImpl(utilisateurDAO);
        this.filiereService     = new FiliereServiceImpl(filiereDAO);
        this.etudiantService    = new EtudiantServiceImpl(etudiantDAO, promotionDAO);
        this.moduleService      = new ModuleServiceImpl(moduleDAO, promotionDAO);
        this.noteService        = new NoteServiceImpl(noteDAO, etudiantDAO, moduleDAO);
        this.statistiqueService = new StatistiqueServiceImpl(
                statistiqueDAO, etudiantDAO, noteDAO, moduleDAO, promotionDAO);
    }

    public AuthService        getAuthService()        { return authService; }
    public FiliereService     getFiliereService()     { return filiereService; }
    public EtudiantService    getEtudiantService()    { return etudiantService; }
    public ModuleService      getModuleService()      { return moduleService; }
    public NoteService        getNoteService()        { return noteService; }
    public StatistiqueService getStatistiqueService() { return statistiqueService; }
}
