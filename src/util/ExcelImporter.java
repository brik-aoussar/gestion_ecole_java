package util;

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
 * Utilitaire d'import Excel via Apache POI.
 * Dépendance Maven : org.apache.poi:poi-ooxml:5.2.5
 *
 * Colonnes attendues pour étudiants  : CNE | Nom | Prénom | Date naissance | Email
 * Colonnes attendues pour notes      : CNE étudiant | Note | Type note
 */
public class ExcelImporter {

    private ExcelImporter() {}

    // ── IMPORT ÉTUDIANTS ──────────────────────────────────────────────────────
    public static List<Etudiant> lireEtudiants(File fichier) {
        List<Etudiant> liste = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(fichier);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            int premiere = sheet.getFirstRowNum() + 1; // ligne 0 = en-tête

            for (int i = premiere; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || estVide(row)) continue;

                Etudiant e = new Etudiant();
                e.setCne(getString(row, 0));
                e.setNom(getString(row, 1));
                e.setPrenom(getString(row, 2));
                e.setDateNaissance(getDate(row, 3));
                e.setEmail(getString(row, 4));
                liste.add(e);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lecture fichier Excel étudiants", ex);
        }
        return liste;
    }

    // ── IMPORT NOTES ──────────────────────────────────────────────────────────
    public static List<Note> lireNotes(File fichier, Long sousModuleId, Long enseignantId) {
        List<Note> liste = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(fichier);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            int premiere = sheet.getFirstRowNum() + 1;

            for (int i = premiere; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || estVide(row)) continue;

                // Col 0 : etudiant_id (Long), Col 1 : valeur, Col 2 : type note
                long etudiantId = (long) getNumeric(row, 0);
                double valeur   = getNumeric(row, 1);
                String typeStr  = getString(row, 2);

                Note.TypeNote type;
                try { type = Note.TypeNote.valueOf(typeStr.toUpperCase()); }
                catch (Exception ex) { type = Note.TypeNote.EXAMEN; }

                Note n = new Note(valeur, type, etudiantId, sousModuleId, enseignantId);
                liste.add(n);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Erreur lecture fichier Excel notes", ex);
        }
        return liste;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private static String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> "";
        };
    }

    private static double getNumeric(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return 0.0;
        return cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : 0.0;
    }

    private static LocalDate getDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date d = cell.getDateCellValue();
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private static boolean estVide(Row row) {
        for (Cell c : row) {
            if (c != null && c.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}
