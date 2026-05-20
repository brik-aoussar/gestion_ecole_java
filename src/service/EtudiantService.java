package service;

import model.Etudiant;
import java.io.File;
import java.util.List;

public interface EtudiantService {
    Etudiant      ajouter(Etudiant e);
    Etudiant      modifier(Etudiant e);
    void          archiver(Long id);
    List<Etudiant> recupererTous();
    List<Etudiant> recupererParPromotion(Long promotionId);
    List<Etudiant> recupererParFiliere(Long filiereId);
    List<Etudiant> rechercher(String terme);
    int            importerDepuisExcel(File fichier, Long promotionId);
}
