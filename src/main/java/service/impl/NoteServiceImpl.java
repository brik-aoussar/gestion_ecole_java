package service.impl;

import dao.EtudiantDAO;
import dao.ModuleDAO;
import dao.NoteDAO;
import exception.ServiceException;
import exception.ValidationException;
import model.Etudiant;
import model.Note;
import model.SousModule;
import service.NoteService;
import util.ExcelImporter;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Logique métier notes. ZÉRO SQL ici.
 */
public class NoteServiceImpl implements NoteService {

    private final NoteDAO     noteDAO;
    private final EtudiantDAO etudiantDAO;
    private final ModuleDAO   moduleDAO;

    public NoteServiceImpl(NoteDAO noteDAO, EtudiantDAO etudiantDAO, ModuleDAO moduleDAO) {
        this.noteDAO     = noteDAO;
        this.etudiantDAO = etudiantDAO;
        this.moduleDAO   = moduleDAO;
    }

    @Override
    public Note saisirNote(Note n) {
        validerNote(n);
        verifierEtudiantActif(n.getEtudiantId());
        verifierSousModuleExiste(n.getSousModuleId());
        verifierEnseignantAssigne(n.getSousModuleId(), n.getSaisiPar());
        try {
            // upsert : ON DUPLICATE KEY UPDATE — gère aussi les modifications
            noteDAO.upsert(n);
            return n;
        } catch (SQLException ex) { throw new ServiceException("Erreur saisie note", ex); }
    }

    @Override
    public Note modifierNote(Note n) {
        if (n.getId() == null)
            throw new ValidationException("id", "Identifiant note obligatoire");
        validerNote(n);
        verifierEnseignantAssigne(n.getSousModuleId(), n.getSaisiPar());
        try {
            noteDAO.findById(n.getId())
                    .orElseThrow(() -> new ServiceException("Note introuvable : id=" + n.getId()));
            noteDAO.update(n);
            return n;
        } catch (SQLException ex) { throw new ServiceException("Erreur modification note", ex); }
    }

    @Override
    public void supprimerNote(Long id) {
        try {
            noteDAO.findById(id)
                    .orElseThrow(() -> new ServiceException("Note introuvable : id=" + id));
            noteDAO.delete(id);
        } catch (SQLException ex) { throw new ServiceException("Erreur suppression note", ex); }
    }

    @Override
    public int importerNotes(File fichier, Long sousModuleId, Long enseignantId) {
        if (fichier == null || !fichier.exists())
            throw new ValidationException("fichier", "Fichier introuvable");
        if (!fichier.getName().matches(".*\\.xlsx?"))
            throw new ValidationException("fichier", "Format attendu : .xls ou .xlsx");
        verifierSousModuleExiste(sousModuleId);
        verifierEnseignantAssigne(sousModuleId, enseignantId);
        try {
            List<Note> notes = ExcelImporter.importNotes(fichier, sousModuleId, enseignantId);
            if (notes.isEmpty()) throw new ServiceException("Fichier Excel vide ou invalide");
            notes.forEach(this::validerNote);
            notes.forEach(n -> verifierEtudiantActif(n.getEtudiantId()));
            return noteDAO.insertBatch(notes);
        } catch (SQLException ex) { throw new ServiceException("Erreur import notes", ex); }
    }

    @Override
    public void validerNotesSousModule(Long sousModuleId) {
        verifierSousModuleExiste(sousModuleId);
        try {
            List<Note> notes = noteDAO.findBySousModule(sousModuleId);
            if (notes.isEmpty())
                throw new ServiceException("Aucune note saisie pour ce sous-module");
            // Vérifie que tous les étudiants de la promotion ont une note
            SousModule sm = moduleDAO.findSousModuleById(sousModuleId)
                    .orElseThrow(() -> new ServiceException("Sous-module introuvable"));
            List<Etudiant> etudiants = etudiantDAO.findByPromotion(
                    moduleDAO.findModuleById(sm.getModuleId())
                             .orElseThrow(() -> new ServiceException("Module introuvable"))
                             .getPromotionId());
            long notesCount = notes.stream()
                    .map(Note::getEtudiantId).distinct().count();
            if (notesCount < etudiants.size())
                throw new ServiceException(
                        "Notes incomplètes : " + notesCount + "/" + etudiants.size() +
                        " étudiants ont une note. Complétez avant de valider.");
        } catch (SQLException ex) { throw new ServiceException("Erreur validation notes", ex); }
    }

    @Override
    public List<Note> getNotesParEtudiant(Long etudiantId) {
        verifierEtudiantActif(etudiantId);
        try { return noteDAO.findByEtudiant(etudiantId); }
        catch (SQLException ex) { throw new ServiceException("Erreur récupération notes", ex); }
    }

    @Override
    public List<Note> getNotesParSousModule(Long sousModuleId) {
        verifierSousModuleExiste(sousModuleId);
        try { return noteDAO.findBySousModule(sousModuleId); }
        catch (SQLException ex) { throw new ServiceException("Erreur récupération notes", ex); }
    }

    @Override
    public double getMoyennePonderee(Long etudiantId, Long promotionId) {
        try {
            List<Note> notes = noteDAO.findByEtudiant(etudiantId);
            if (notes.isEmpty()) return 0.0;

            double sommeNoteCoeff = 0.0;
            double sommeCoeff     = 0.0;

            for (Note n : notes) {
                SousModule sm = moduleDAO.findSousModuleById(n.getSousModuleId())
                        .orElse(null);
                if (sm == null) continue;
                double coeff = sm.getCoefficient();
                sommeNoteCoeff += n.getValeur() * coeff;
                sommeCoeff     += coeff;
            }
            return sommeCoeff == 0 ? 0.0 :
                   Math.round((sommeNoteCoeff / sommeCoeff) * 100.0) / 100.0;
        } catch (SQLException ex) { throw new ServiceException("Erreur calcul moyenne", ex); }
    }

    // ── VALIDATIONS ───────────────────────────────────────────────────────────
    private void validerNote(Note n) {
        if (n.getValeur() < 0.0 || n.getValeur() > 20.0)
            throw new ValidationException("valeur", "La note doit être comprise entre 0 et 20");
        if (n.getTypeNote() == null)
            throw new ValidationException("typeNote", "Le type de note est obligatoire");
        if (n.getEtudiantId() == null)
            throw new ValidationException("etudiantId", "L'étudiant est obligatoire");
        if (n.getSousModuleId() == null)
            throw new ValidationException("sousModuleId", "Le sous-module est obligatoire");
    }

    private void verifierEtudiantActif(Long etudiantId) {
        try {
            Etudiant e = etudiantDAO.findById(etudiantId)
                    .orElseThrow(() -> new ServiceException("Étudiant introuvable : id=" + etudiantId));
            if (!e.isActif())
                throw new ServiceException("Étudiant archivé ou suspendu : id=" + etudiantId);
        } catch (SQLException ex) { throw new ServiceException("Erreur vérification étudiant", ex); }
    }

    private void verifierSousModuleExiste(Long sousModuleId) {
        try {
            moduleDAO.findSousModuleById(sousModuleId)
                    .orElseThrow(() -> new ServiceException("Sous-module introuvable : id=" + sousModuleId));
        } catch (SQLException ex) { throw new ServiceException("Erreur vérification sous-module", ex); }
    }

    private void verifierEnseignantAssigne(Long sousModuleId, Long enseignantId) {
        if (enseignantId == null) return; // responsable planning peut saisir sans être assigné
        try {
            SousModule sm = moduleDAO.findSousModuleById(sousModuleId)
                    .orElseThrow(() -> new ServiceException("Sous-module introuvable"));
            if (sm.getEnseignantId() != null && !sm.getEnseignantId().equals(enseignantId))
                throw new ServiceException(
                        "L'enseignant (id=" + enseignantId + ") n'est pas assigné à ce sous-module");
        } catch (SQLException ex) { throw new ServiceException("Erreur vérification enseignant", ex); }
    }
}
