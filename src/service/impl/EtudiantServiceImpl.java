package service.impl;

import dao.EtudiantDAO;
import dao.PromotionDAO;
import exception.ServiceException;
import exception.ValidationException;
import model.Etudiant;
import service.EtudiantService;
import util.ExcelImporter;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Logique métier étudiants. ZÉRO SQL ici.
 */
public class EtudiantServiceImpl implements EtudiantService {

    private static final Pattern EMAIL_RE =
            Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$");

    private final EtudiantDAO  etudiantDAO;
    private final PromotionDAO promotionDAO;

    public EtudiantServiceImpl(EtudiantDAO etudiantDAO, PromotionDAO promotionDAO) {
        this.etudiantDAO  = etudiantDAO;
        this.promotionDAO = promotionDAO;
    }

    @Override
    public Etudiant ajouter(Etudiant e) {
        valider(e, true);
        try { return etudiantDAO.insert(e); }
        catch (SQLException ex) { throw new ServiceException("Erreur ajout étudiant", ex); }
    }

    @Override
    public Etudiant modifier(Etudiant e) {
        if (e.getId() == null)
            throw new ValidationException("id", "Identifiant obligatoire pour modification");
        valider(e, false);
        try { etudiantDAO.update(e); return e; }
        catch (SQLException ex) { throw new ServiceException("Erreur modification étudiant", ex); }
    }

    @Override
    public void archiver(Long id) {
        try {
            Etudiant e = etudiantDAO.findById(id)
                    .orElseThrow(() -> new ServiceException("Étudiant introuvable : id=" + id));
            if (e.getStatut() == Etudiant.Statut.ARCHIVE)
                throw new ServiceException("Étudiant déjà archivé");
            etudiantDAO.archiver(id);
        } catch (SQLException ex) { throw new ServiceException("Erreur archivage", ex); }
    }

    @Override
    public List<Etudiant> recupererTous() {
        try { return etudiantDAO.findAllActifs(); }
        catch (SQLException ex) { throw new ServiceException("Erreur récupération étudiants", ex); }
    }

    @Override
    public List<Etudiant> recupererParPromotion(Long promotionId) {
        verifierPromotion(promotionId);
        try { return etudiantDAO.findByPromotion(promotionId); }
        catch (SQLException ex) { throw new ServiceException("Erreur récupération par promotion", ex); }
    }

    @Override
    public List<Etudiant> recupererParFiliere(Long filiereId) {
        try {
            return promotionDAO.findByFiliere(filiereId).stream()
                    .flatMap(p -> {
                        try { return etudiantDAO.findByPromotion(p.getId()).stream(); }
                        catch (SQLException ex) { throw new ServiceException("Erreur filière", ex); }
                    })
                    .distinct().toList();
        } catch (SQLException ex) { throw new ServiceException("Erreur récupération par filière", ex); }
    }

    @Override
    public List<Etudiant> rechercher(String terme) {
        if (terme == null || terme.isBlank())
            throw new ValidationException("terme", "Terme de recherche vide");
        try { return etudiantDAO.rechercher(terme.trim()); }
        catch (SQLException ex) { throw new ServiceException("Erreur recherche", ex); }
    }

    @Override
    public int importerDepuisExcel(File fichier, Long promotionId) {
        if (fichier == null || !fichier.exists())
            throw new ValidationException("fichier", "Fichier introuvable");
        if (!fichier.getName().matches(".*\\.xlsx?"))
            throw new ValidationException("fichier", "Format attendu : .xls ou .xlsx");
        verifierPromotion(promotionId);
        try {
            List<Etudiant> liste = ExcelImporter.lireEtudiants(fichier);
            if (liste.isEmpty()) throw new ServiceException("Fichier Excel vide ou invalide");
            liste.forEach(e -> valider(e, false)); // validation légère (pas unicité CNE au bulk)
            return etudiantDAO.insertBatch(liste, promotionId);
        } catch (SQLException ex) { throw new ServiceException("Erreur import Excel", ex); }
    }

    // ── VALIDATIONS ───────────────────────────────────────────────────────────
    private void valider(Etudiant e, boolean checkUnique) {
        if (e.getNom() == null || e.getNom().trim().length() < 2)
            throw new ValidationException("nom", "Minimum 2 caractères");
        if (e.getPrenom() == null || e.getPrenom().trim().length() < 2)
            throw new ValidationException("prenom", "Minimum 2 caractères");
        if (e.getCne() == null || e.getCne().isBlank())
            throw new ValidationException("cne", "CNE obligatoire");
        if (e.getEmail() == null || e.getEmail().isBlank())
            throw new ValidationException("email", "Email obligatoire");
        if (!EMAIL_RE.matcher(e.getEmail()).matches())
            throw new ValidationException("email", "Format email invalide");
        if (e.getDateNaissance() != null && !e.getDateNaissance().isBefore(LocalDate.now()))
            throw new ValidationException("dateNaissance", "Date de naissance doit être dans le passé");
        if (checkUnique) {
            try {
                if (etudiantDAO.findByCne(e.getCne()).isPresent())
                    throw new ServiceException("CNE déjà utilisé : " + e.getCne());
            } catch (SQLException ex) { throw new ServiceException("Erreur vérification CNE", ex); }
        }
    }

    private void verifierPromotion(Long id) {
        try {
            promotionDAO.findById(id)
                    .orElseThrow(() -> new ServiceException("Promotion introuvable : id=" + id));
        } catch (SQLException ex) { throw new ServiceException("Erreur vérification promotion", ex); }
    }
}
