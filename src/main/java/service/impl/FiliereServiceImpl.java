package service.impl;

import dao.FiliereDAO;
import exception.ServiceException;
import exception.ValidationException;
import model.Filiere;
import service.FiliereService;

import java.sql.SQLException;
import java.util.List;

/**
 * Logique métier filières. ZÉRO SQL ici.
 */
public class FiliereServiceImpl implements FiliereService {

    private final FiliereDAO filiereDAO;

    public FiliereServiceImpl(FiliereDAO filiereDAO) {
        this.filiereDAO = filiereDAO;
    }

    @Override
    public Filiere ajouter(Filiere f) {
        valider(f);
        try {
            // Unicité du code
            if (filiereDAO.findByCode(f.getCode()).isPresent())
                throw new ServiceException("Code filière déjà utilisé : " + f.getCode());
            return filiereDAO.insert(f);
        } catch (SQLException ex) { throw new ServiceException("Erreur ajout filière", ex); }
    }

    @Override
    public Filiere modifier(Filiere f) {
        if (f.getId() == null)
            throw new ValidationException("id", "Identifiant obligatoire");
        valider(f);
        try { filiereDAO.update(f); return f; }
        catch (SQLException ex) { throw new ServiceException("Erreur modification filière", ex); }
    }

    @Override
    public void supprimer(Long id) {
        try {
            filiereDAO.findById(id)
                    .orElseThrow(() -> new ServiceException("Filière introuvable : id=" + id));
            // Vérifie qu'aucune promotion active n'est attachée
            if (filiereDAO.hasPromotionsActives(id))
                throw new ServiceException("Impossible de supprimer : la filière contient des promotions actives");
            filiereDAO.delete(id);
        } catch (SQLException ex) { throw new ServiceException("Erreur suppression filière", ex); }
    }

    @Override
    public List<Filiere> recupererToutes() {
        try { return filiereDAO.findAll(); }
        catch (SQLException ex) { throw new ServiceException("Erreur récupération filières", ex); }
    }

    @Override
    public Filiere recupererParId(Long id) {
        try {
            return filiereDAO.findById(id)
                    .orElseThrow(() -> new ServiceException("Filière introuvable : id=" + id));
        } catch (SQLException ex) { throw new ServiceException("Erreur récupération filière", ex); }
    }

    @Override
    public List<Filiere> recupererParResponsable(Long responsableFiliereId) {
        try { return filiereDAO.findByResponsable(responsableFiliereId); }
        catch (SQLException ex) { throw new ServiceException("Erreur récupération filières du responsable", ex); }
    }

    // ── VALIDATIONS ───────────────────────────────────────────────────────────
    private void valider(Filiere f) {
        if (f.getCode() == null || f.getCode().isBlank())
            throw new ValidationException("code", "Code filière obligatoire");
        if (f.getCode().length() > 20)
            throw new ValidationException("code", "Code filière max 20 caractères");
        if (f.getIntitule() == null || f.getIntitule().trim().length() < 3)
            throw new ValidationException("intitule", "Intitulé minimum 3 caractères");
    }
}
