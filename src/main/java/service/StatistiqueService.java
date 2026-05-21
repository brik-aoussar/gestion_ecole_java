package service;

import java.util.List;
import java.util.Map;

public interface StatistiqueService {
    double                    calculerMoyenneGenerale(Long etudiantId, Long promotionId);
    List<Map<String, Object>> getClassement(Long promotionId);
    List<Map<String, Object>> getEtudiantsEnEchec(Long promotionId);
    Map<String, Object>       getMeilleurEtudiant(Long promotionId);
    double                    getTauxReussite(Long promotionId);
    Map<String, Object>       getRapportParPromotion(Long promotionId);
    Map<String, Object>       getRapportParFiliere(Long filiereId);
}
