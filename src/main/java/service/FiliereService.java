package service;

import model.Filiere;
import java.util.List;

public interface FiliereService {
    Filiere       ajouter(Filiere f);
    Filiere       modifier(Filiere f);
    void          supprimer(Long id);
    List<Filiere> recupererToutes();
    Filiere       recupererParId(Long id);
    List<Filiere> recupererParResponsable(Long responsableFiliereId);
}
