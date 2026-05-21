package util;

import config.Constantes;
import exception.ValidationException;
import model.Etudiant;
import model.Note;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Import Excel via Apache POI (poi-ooxml ≥ 5.2).
 *
 * Colonnes étudiants  : CNE | Nom | Prénom | Date naissance (jj/MM/aaaa) | Email | Téléphone
 * Colonnes notes      : ID étudiant | Note | Type (EXAMEN/TP/CC/PROJET)
 */
public final class ExcelImporter {

    private ExcelImporter() {}

    // ── IMPORT ÉTUDIANTS ──────────────────────────────────────────────────────

    /**
     * Lit un fichier Excel et retourne la liste des étudiants.
     * Ignore les lignes vides. Lève ValidationException si le fichier est illisible.
     *
     * @param fichier fichier .xlsx ou .xls
     * @return liste d'Etudiant (sans id — à persister via DAO)
     */
    public static List<Etudiant> importEtudiants(File fichier) {
        validerFichier(fichier);
        List<Etudiant> liste = new ArrayList<>();
        List<String>   erreurs = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(fichier);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);

            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || estLigneVide(row)) continue;

                try {
                    Etudiant e = new Etudiant();
                    e.setCne(   getString(row, 0).toUpperCase());
                    e.setNom(   capitaliser(getString(row, 1)));
                    e.setPrenom(capitaliser(getString(row, 2)));
                    e.setDateNaissance(getDate(row, 3));
                    e.setEmail( getString(row, 4).toLowerCase());
                    e.setTelephone(getString(row, 5));
                    e.setStatut(Etudiant.Statut.ACTIF);

                    // Validation rapide par ligne
                    validerLigneEtudiant(e, i + 1);
                    liste.add(e);

                } catch (ValidationException ex) {
                    erreurs.add("Ligne " + (i + 1) + " : " + ex.getMessage());
                }
            }

        } catch (IOException ex) {
            throw new ValidationException("fichier",
                    "Impossible de lire le fichier Excel : " + ex.getMessage());
        }

        if (!erreurs.isEmpty())
            throw new ValidationException("fichier",
                    erreurs.size() + " ligne(s) invalide(s) :\n" + String.join("\n", erreurs));

        return liste;
    }

    // ── IMPORT NOTES ──────────────────────────────────────────────────────────

    /**
     * Lit un fichier Excel et retourne la liste des notes pour un sous-module.
     *
     * Colonnes : ID étudiant | Note (0-20) | Type note (optionnel — défaut EXAMEN)
     *
     * @param fichier      fichier .xlsx ou .xls
     * @param sousModuleId id du sous-module cible
     * @param enseignantId id de l'enseignant qui importe
     * @return liste de Note (sans id — à persister via DAO)
     */
    public static List<Note> importNotes(File fichier, Long sousModuleId, Long enseignantId) {
        validerFichier(fichier);
        List<Note>   liste   = new ArrayList<>();
        List<String> erreurs = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(fichier);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);

            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || estLigneVide(row)) continue;

                try {
                    long   etudiantId = (long) getNumeric(row, 0);
                    double valeur     = getNumeric(row, 1);
                    String typeStr    = getString(row, 2);

                    if (etudiantId <= 0)
                        throw new ValidationException("etudiant_id", "ID étudiant invalide");

                    if (!Validator.isValidNote(valeur))
                        throw new ValidationException("note",
                                Constantes.MSG_NOTE_INVALIDE + " (valeur=" + valeur + ")");

                    Note.TypeNote type = parseTypeNote(typeStr);
                    liste.add(new Note(valeur, type, etudiantId, sousModuleId, enseignantId));

                } catch (ValidationException ex) {
                    erreurs.add("Ligne " + (i + 1) + " : " + ex.getMessage());
                }
            }

        } catch (IOException ex) {
            throw new ValidationException("fichier",
                    "Impossible de lire le fichier Excel : " + ex.getMessage());
        }

        if (!erreurs.isEmpty())
            throw new ValidationException("fichier",
                    erreurs.size() + " ligne(s) invalide(s) :\n" + String.join("\n", erreurs));

        return liste;
    }

    // ── VALIDATIONS PRIVÉES ───────────────────────────────────────────────────

    private static void validerFichier(File fichier) {
        if (fichier == null || !fichier.exists())
            throw new ValidationException("fichier", "Fichier introuvable");
        if (!fichier.getName().matches(".*\\.xlsx?"))
            throw new ValidationException("fichier", "Format attendu : .xlsx ou .xls");
        if (fichier.length() == 0)
            throw new ValidationException("fichier", "Le fichier est vide");
    }

    private static void validerLigneEtudiant(Etudiant e, int ligne) {
        if (!Validator.isNotEmpty(e.getCne()))
            throw new ValidationException("cne", "CNE obligatoire (ligne " + ligne + ")");
        if (!Validator.isMinLength(e.getNom(), 2))
            throw new ValidationException("nom", "Nom trop court (ligne " + ligne + ")");
        if (!Validator.isMinLength(e.getPrenom(), 2))
            throw new ValidationException("prenom", "Prénom trop court (ligne " + ligne + ")");
        if (!Validator.isValidEmail(e.getEmail()))
            throw new ValidationException("email", "Email invalide (ligne " + ligne + ")");
    }

    // ── HELPERS LECTURE CELLULES ──────────────────────────────────────────────

    private static String getString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                            ? "" : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.STRING
                            ? cell.getRichStringCellValue().getString().trim()
                            : String.valueOf((long) cell.getNumericCellValue());
            default      -> "";
        };
    }

    private static double getNumeric(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return 0.0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> {
                try { yield Double.parseDouble(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield 0.0; }
            }
            default -> 0.0;
        };
    }

    private static LocalDate getDate(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        // Tentative parsing texte jj/MM/aaaa
        String txt = getString(row, col);
        if (!txt.isEmpty()) {
            try {
                String[] parts = txt.split("[/\\-.]");
                if (parts.length == 3)
                    return LocalDate.of(
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[0]));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static boolean estLigneVide(Row row) {
        for (Cell c : row)
            if (c != null && c.getCellType() != CellType.BLANK
                          && !getString(row, c.getColumnIndex()).isEmpty())
                return false;
        return true;
    }

    private static Note.TypeNote parseTypeNote(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) return Note.TypeNote.EXAMEN;
        try { return Note.TypeNote.valueOf(typeStr.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { return Note.TypeNote.EXAMEN; }
    }

    private static String capitaliser(String s) {
        if (s == null || s.isBlank()) return s;
        String t = s.trim().toLowerCase();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
