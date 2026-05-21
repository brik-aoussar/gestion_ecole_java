package service;

import model.Note;
import java.io.File;
import java.util.List;

public interface NoteService {
    Note        saisirNote(Note n);
    Note        modifierNote(Note n);
    void        supprimerNote(Long id);
    int         importerNotes(File fichier, Long sousModuleId, Long enseignantId);
    void        validerNotesSousModule(Long sousModuleId);
    List<Note>  getNotesParEtudiant(Long etudiantId);
    List<Note>  getNotesParSousModule(Long sousModuleId);
    double      getMoyennePonderee(Long etudiantId, Long promotionId);
}
