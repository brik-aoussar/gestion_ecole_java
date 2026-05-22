package service.impl;

import dao.EtudiantDAO;
import dao.ModuleDAO;
import dao.NoteDAO;
import dao.PromotionDAO;
import dao.StatistiqueDAO;
import exception.ServiceException;
import exception.ValidationException;
import model.Etudiant;
import model.Note;
import model.SousModule;
import service.StatistiqueService;

import java.sql.SQLException;
import java.util.*;

/**
 * Calculs statistiques et rapports. ZÉRO SQL ici.
 * S'appuie sur StatistiqueDAO (vues SQL) + calculs Java pour les règles métier.
 */
public class StatistiqueServiceImpl implements StatistiqueService {

    private static final double SEUIL_REUSSITE  = 10.0;
    private static final double COEFF_ELIMINATOIRE = 4.0; // coefficient critique

    private final StatistiqueDAO statistiqueDAO;
    private final EtudiantDAO    etudiantDAO;
    private final NoteDAO        noteDAO;
    private final ModuleDAO      moduleDAO;
    private final PromotionDAO   promotionDAO;

    public StatistiqueServiceImpl(StatistiqueDAO statistiqueDAO,
                                  EtudiantDAO etudiantDAO,
                                  NoteDAO noteDAO,
                                  ModuleDAO moduleDAO,
                                  PromotionDAO promotionDAO) {
        this.statistiqueDAO = statistiqueDAO;
        this.etudiantDAO    = etudiantDAO;
        this.noteDAO        = noteDAO;
        this.moduleDAO      = moduleDAO;
        this.promotionDAO   = promotionDAO;
    }

    // ── MOYENNE GÉNÉRALE ─────────────────────────────────────────────────────
    @Override
    public double calculerMoyenneGenerale(Long etudiantId, Long promotionId) {
        verifierEtudiant(etudiantId);
        verifierPromotion(promotionId);
        try {
            return statistiqueDAO.getMoyenneGenerale(etudiantId, promotionId)
                    .orElse(0.0);
        } catch (SQLException ex) {
            throw new ServiceException("Erreur calcul moyenne générale", ex);
        }
    }

    // ── CLASSEMENT d'une promotion ────────────────────────────────────────────
    @Override
    public List<Map<String, Object>> getClassement(Long promotionId) {
        verifierPromotion(promotionId);
        try {
            List<Map<String, Object>> classement = statistiqueDAO.getClassementPromotion(promotionId);
            // Ajoute mention selon moyenne
            classement.forEach(row -> {
                double moy = toDouble(row.get("moyenne_generale"));
                row.put("mention", getMention(moy));
            });
            return classement;
        } catch (SQLException ex) {
            throw new ServiceException("Erreur calcul classement", ex);
        }
    }

    // ── TAUX DE RÉUSSITE ─────────────────────────────────────────────────────
    @Override
    public double getTauxReussite(Long promotionId) {
        verifierPromotion(promotionId);
        try {
            List<Etudiant> etudiants = etudiantDAO.findByPromotion(promotionId);
            if (etudiants.isEmpty()) return 0.0;

            long nbReussis = etudiants.stream()
                    .filter(e -> {
                        try {
                            double moy = statistiqueDAO
                                    .getMoyenneGenerale(e.getId(), promotionId)
                                    .orElse(0.0);
                            return moy >= SEUIL_REUSSITE && !aUneNoteEliminatoire(e.getId());
                        } catch (SQLException ex) {
                            throw new ServiceException("Erreur calcul réussite", ex);
                        }
                    }).count();

            double taux = (double) nbReussis / etudiants.size() * 100.0;
            return Math.round(taux * 100.0) / 100.0;
        } catch (SQLException ex) {
            throw new ServiceException("Erreur calcul taux de réussite", ex);
        }
    }

    // ── ÉTUDIANTS EN ÉCHEC ────────────────────────────────────────────────────
    // Échec = moyenne générale < 10 OU note < 10 dans un module à coefficient ≥ 4
    @Override
    public List<Map<String, Object>> getEtudiantsEnEchec(Long promotionId) {
        verifierPromotion(promotionId);
        try {
            // Étudiants sous le seuil via vue SQL
            List<Map<String, Object>> parMoyenne =
                    statistiqueDAO.getEtudiantsEnEchec(promotionId, SEUIL_REUSSITE);

            // Ajoute ceux qui ont une note éliminatoire même si moy >= 10
            List<Etudiant> tous = etudiantDAO.findByPromotion(promotionId);
            Set<Long> dejaEnEchec = new HashSet<>();
            parMoyenne.forEach(r -> dejaEnEchec.add(toLong(r.get("etudiant_id"))));

            for (Etudiant e : tous) {
                if (!dejaEnEchec.contains(e.getId()) && aUneNoteEliminatoire(e.getId())) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("etudiant_id",      e.getId());
                    row.put("etudiant",          e.getNomComplet());
                    row.put("moyenne_generale",  calculerMoyenneGenerale(e.getId(), promotionId));
                    row.put("motif_echec",       "Note éliminatoire (coeff ≥ " + COEFF_ELIMINATOIRE + ")");
                    parMoyenne.add(row);
                }
            }

            parMoyenne.forEach(r -> r.putIfAbsent("motif_echec", "Moyenne générale < 10"));
            return parMoyenne;

        } catch (SQLException ex) {
            throw new ServiceException("Erreur récupération étudiants en échec", ex);
        }
    }

    // ── RAPPORT COMPLET par promotion ─────────────────────────────────────────
    @Override
    public List<Map<String, Object>> getRapportParPromotion(Long promotionId) {
        verifierPromotion(promotionId);
        try {
            List<Map<String, Object>> classement = getClassement(promotionId);
            double tauxReussite = getTauxReussite(promotionId);

            // Enrichit chaque ligne avec le statut
            classement.forEach(row -> {
                double moy = toDouble(row.get("moyenne_generale"));
                Long etId  = toLong(row.get("etudiant_id"));
                boolean eliminatoire;
                try { eliminatoire = aUneNoteEliminatoire(etId); }
                catch (Exception ex) { eliminatoire = false; }
                row.put("statut", (moy >= SEUIL_REUSSITE && !eliminatoire) ? "ADMIS" : "ECHEC");
            });

            // Résumé global en tête de liste
            Map<String, Object> resume = new LinkedHashMap<>();
            resume.put("promotion_id",   promotionId);
            resume.put("total_etudiants", classement.size());
            resume.put("taux_reussite",   tauxReussite + "%");
            resume.put("meilleure_moy",
                classement.stream().mapToDouble(r -> toDouble(r.get("moyenne_generale"))).max().orElse(0.0));
            resume.put("type", "RESUME");
            classement.add(0, resume);

            return classement;
        } catch (Exception ex) {
            throw new ServiceException("Erreur génération rapport", ex);
        }
    }

    // ── MEILLEUR ÉTUDIANT d'une promotion ────────────────────────────────────
    @Override
    public Map<String, Object> getMeilleurEtudiant(Long promotionId) {
        verifierPromotion(promotionId);
        try {
            return statistiqueDAO.getMeilleurEtudiant(promotionId).orElse(null);
        } catch (SQLException ex) {
            throw new ServiceException("Erreur récupération meilleur étudiant", ex);
        }
    }

    // ── RAPPORT COMPLET par filière ───────────────────────────────────────────
    @Override
    public List<Map<String, Object>> getRapportParFiliere(Long filiereId) {
        if (filiereId == null) throw new ValidationException("filiereId", "Identifiant filière obligatoire");
        try {
            List<Map<String, Object>> lignes = statistiqueDAO.getClassementPromotion(filiereId);

            Map<String, Object> resume = new LinkedHashMap<>();
            resume.put("filiere_id",      filiereId);
            resume.put("total_etudiants", lignes.size());
            resume.put("type",            "RESUME_FILIERE");
            lignes.add(0, resume);

            return lignes;
        } catch (SQLException ex) {
            throw new ServiceException("Erreur génération rapport filière", ex);
        }
    }

    // ── FICHE INDIVIDUELLE ────────────────────────────────────────────────────
    @Override
    public Map<String, Object> getFicheEtudiant(Long etudiantId, Long promotionId) {
        verifierEtudiant(etudiantId);
        verifierPromotion(promotionId);
        try {
            Map<String, Object> fiche = new LinkedHashMap<>();
            Etudiant e = etudiantDAO.findById(etudiantId)
                    .orElseThrow(() -> new ServiceException("Étudiant introuvable"));

            double moyGenerale = calculerMoyenneGenerale(etudiantId, promotionId);

            fiche.put("etudiant_id",      e.getId());
            fiche.put("cne",              e.getCne());
            fiche.put("nom_complet",      e.getNomComplet());
            fiche.put("moyenne_generale", moyGenerale);
            fiche.put("mention",          getMention(moyGenerale));
            fiche.put("statut",           moyGenerale >= SEUIL_REUSSITE
                                          && !aUneNoteEliminatoire(etudiantId) ? "ADMIS" : "ECHEC");
            fiche.put("moyennes_modules", statistiqueDAO.getMoyennesModuleParEtudiant(etudiantId));
            fiche.put("detail_notes",     statistiqueDAO.getFicheResultatEtudiant(etudiantId, promotionId));

            return fiche;
        } catch (SQLException ex) {
            throw new ServiceException("Erreur génération fiche étudiant", ex);
        }
    }

    // ── HELPERS PRIVÉS ────────────────────────────────────────────────────────

    /**
     * Vérifie si l'étudiant a une note < 10 dans un sous-module
     * dont le coefficient du MODULE parent est ≥ COEFF_ELIMINATOIRE.
     */
    private boolean aUneNoteEliminatoire(Long etudiantId) throws SQLException {
        List<Note> notes = noteDAO.findByEtudiant(etudiantId);
        for (Note n : notes) {
            SousModule sm = moduleDAO.findSousModuleById(n.getSousModuleId()).orElse(null);
            if (sm == null) continue;
            double coeffModule = moduleDAO.findModuleById(sm.getModuleId())
                    .map(model.Module::getCoefficient).orElse(0.0);
            if (coeffModule >= COEFF_ELIMINATOIRE && n.getValeur() < SEUIL_REUSSITE)
                return true;
        }
        return false;
    }

    private String getMention(double moy) {
        if (moy >= 16) return "Très Bien";
        if (moy >= 14) return "Bien";
        if (moy >= 12) return "Assez Bien";
        if (moy >= 10) return "Passable";
        return "Insuffisant";
    }

    private void verifierEtudiant(Long id) {
        if (id == null) throw new ValidationException("etudiantId", "Identifiant étudiant obligatoire");
    }

    private void verifierPromotion(Long id) {
        if (id == null) throw new ValidationException("promotionId", "Identifiant promotion obligatoire");
        try {
            promotionDAO.findById(id)
                    .orElseThrow(() -> new ServiceException("Promotion introuvable : id=" + id));
        } catch (SQLException ex) { throw new ServiceException("Erreur vérification promotion", ex); }
    }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }
}
