package service;

import model.Module;
import model.SousModule;
import java.util.List;

public interface ModuleService {
    Module       ajouterModule(Module m);
    Module       modifierModule(Module m);
    void         supprimerModule(Long id);
    List<Module> recupererParPromotion(Long promotionId);

    SousModule       ajouterSousModule(SousModule sm);
    SousModule       modifierSousModule(SousModule sm);
    void             supprimerSousModule(Long id);
    List<SousModule> recupererSousModulesParModule(Long moduleId);
    List<SousModule> recupererParEnseignant(Long enseignantId);
    void             assignerEnseignant(Long sousModuleId, Long enseignantId);
}
