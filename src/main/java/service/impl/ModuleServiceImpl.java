package service.impl;

import dao.ModuleDAO;
import dao.PromotionDAO;
import exception.ServiceException;
import exception.ValidationException;
import model.Module;
import model.SousModule;
import service.ModuleService;

import java.sql.SQLException;
import java.util.List;

/**
 * Logique métier modules et sous-modules. ZÉRO SQL ici.
 */
public class ModuleServiceImpl implements ModuleService {

    private final ModuleDAO   moduleDAO;
    private final PromotionDAO promotionDAO;

    public ModuleServiceImpl(ModuleDAO moduleDAO, PromotionDAO promotionDAO) {
        this.moduleDAO   = moduleDAO;
        this.promotionDAO = promotionDAO;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MODULE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public Module ajouterModule(Module m) {
        validerModule(m);
        verifierPromotion(m.getPromotionId());
        try { return moduleDAO.insertModule(m); }
        catch (SQLException ex) { throw new ServiceException("Erreur ajout module", ex); }
    }

    @Override
    public Module modifierModule(Module m) {
        if (m.getId() == null)
            throw new ValidationException("id", "Identifiant obligatoire");
        validerModule(m);
        try {
            moduleDAO.findModuleById(m.getId())
                    .orElseThrow(() -> new ServiceException("Module introuvable : id=" + m.getId()));
            moduleDAO.updateModule(m);
            return m;
        } catch (SQLException ex) { throw new ServiceException("Erreur modification module", ex); }
    }

    @Override
    public void supprimerModule(Long id) {
        try {
            moduleDAO.findModuleById(id)
                    .orElseThrow(() -> new ServiceException("Module introuvable : id=" + id));
            // Vérifie qu'aucun sous-module n'a de notes saisies
            List<SousModule> sousModules = moduleDAO.findByModule(id);
            if (!sousModules.isEmpty())
                throw new ServiceException(
                        "Impossible de supprimer : le module contient " +
                        sousModules.size() + " sous-module(s). Supprimez-les d'abord.");
            moduleDAO.deleteModule(id);
        } catch (SQLException ex) { throw new ServiceException("Erreur suppression module", ex); }
    }

    @Override
    public List<Module> recupererParPromotion(Long promotionId) {
        verifierPromotion(promotionId);
        try { return moduleDAO.findModulesByPromotion(promotionId); }
        catch (SQLException ex) { throw new ServiceException("Erreur récupération modules", ex); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SOUS-MODULE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public SousModule ajouterSousModule(SousModule sm) {
        validerSousModule(sm);
        verifierModuleExiste(sm.getModuleId());
        try { return moduleDAO.insertSousModule(sm); }
        catch (SQLException ex) { throw new ServiceException("Erreur ajout sous-module", ex); }
    }

    @Override
    public SousModule modifierSousModule(SousModule sm) {
        if (sm.getId() == null)
            throw new ValidationException("id", "Identifiant obligatoire");
        validerSousModule(sm);
        try {
            moduleDAO.findSousModuleById(sm.getId())
                    .orElseThrow(() -> new ServiceException("Sous-module introuvable : id=" + sm.getId()));
            moduleDAO.updateSousModule(sm);
            return sm;
        } catch (SQLException ex) { throw new ServiceException("Erreur modification sous-module", ex); }
    }

    @Override
    public void supprimerSousModule(Long id) {
        try {
            moduleDAO.findSousModuleById(id)
                    .orElseThrow(() -> new ServiceException("Sous-module introuvable : id=" + id));
            moduleDAO.deleteSousModule(id);
        } catch (SQLException ex) { throw new ServiceException("Erreur suppression sous-module", ex); }
    }

    @Override
    public List<SousModule> recupererSousModulesParModule(Long moduleId) {
        verifierModuleExiste(moduleId);
        try { return moduleDAO.findByModule(moduleId); }
        catch (SQLException ex) { throw new ServiceException("Erreur récupération sous-modules", ex); }
    }

    @Override
    public List<SousModule> recupererParEnseignant(Long enseignantId) {
        if (enseignantId == null)
            throw new ValidationException("enseignantId", "Identifiant enseignant obligatoire");
        try { return moduleDAO.findByEnseignant(enseignantId); }
        catch (SQLException ex) { throw new ServiceException("Erreur récupération sous-modules enseignant", ex); }
    }

    @Override
    public void assignerEnseignant(Long sousModuleId, Long enseignantId) {
        try {
            moduleDAO.findSousModuleById(sousModuleId)
                    .orElseThrow(() -> new ServiceException("Sous-module introuvable : id=" + sousModuleId));
            moduleDAO.assignerEnseignant(sousModuleId, enseignantId);
        } catch (SQLException ex) { throw new ServiceException("Erreur assignation enseignant", ex); }
    }

    // ── VALIDATIONS ───────────────────────────────────────────────────────────
    private void validerModule(Module m) {
        if (m.getCode() == null || m.getCode().isBlank())
            throw new ValidationException("code", "Code module obligatoire");
        if (m.getIntitule() == null || m.getIntitule().trim().length() < 2)
            throw new ValidationException("intitule", "Intitulé minimum 2 caractères");
        if (m.getCoefficient() <= 0)
            throw new ValidationException("coefficient", "Le coefficient doit être supérieur à 0");
        if (m.getPromotionId() == null)
            throw new ValidationException("promotionId", "La promotion est obligatoire");
    }

    private void validerSousModule(SousModule sm) {
        if (sm.getCode() == null || sm.getCode().isBlank())
            throw new ValidationException("code", "Code sous-module obligatoire");
        if (sm.getIntitule() == null || sm.getIntitule().trim().length() < 2)
            throw new ValidationException("intitule", "Intitulé minimum 2 caractères");
        if (sm.getCoefficient() <= 0)
            throw new ValidationException("coefficient", "Le coefficient doit être supérieur à 0");
        if (sm.getModuleId() == null)
            throw new ValidationException("moduleId", "Le module parent est obligatoire");
    }

    private void verifierPromotion(Long promotionId) {
        try {
            promotionDAO.findById(promotionId)
                    .orElseThrow(() -> new ServiceException("Promotion introuvable : id=" + promotionId));
        } catch (SQLException ex) { throw new ServiceException("Erreur vérification promotion", ex); }
    }

    private void verifierModuleExiste(Long moduleId) {
        try {
            moduleDAO.findModuleById(moduleId)
                    .orElseThrow(() -> new ServiceException("Module introuvable : id=" + moduleId));
        } catch (SQLException ex) { throw new ServiceException("Erreur vérification module", ex); }
    }
}
