package util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import config.AppConfig;
import config.Constantes;
import model.Etudiant;
import model.Note;
import model.Promotion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Export PDF via iText 5.
 * Dépendance Maven : com.itextpdf:itextpdf:5.5.13.3
 *
 * Méthodes publiques :
 *  - exportListeEtudiants(List<Etudiant>, File)
 *  - exportReleveNotes(Etudiant, List<Note>, File)
 *  - exportStatistiquesFiliere(Map<String,Object>, File)
 *  - exporterRapportPromotion(List<Map<String,Object>>, Promotion, File)
 *  - exporterBulletinEtudiant(Map<String,Object>, File)
 */
public final class PDFExporter {

    // ── Palette de couleurs ───────────────────────────────────────────────────
    private static final BaseColor C_ENTETE      = new BaseColor(44,  62,  80);
    private static final BaseColor C_ACCENT      = new BaseColor(41,  128, 185);
    private static final BaseColor C_VERT        = new BaseColor(39,  174, 96);
    private static final BaseColor C_ROUGE       = new BaseColor(192, 57,  43);
    private static final BaseColor C_GRIS_CLAIR  = new BaseColor(236, 240, 241);
    private static final BaseColor C_GRIS_TEXTE  = new BaseColor(80,  80,  80);
    private static final BaseColor C_BLANC       = BaseColor.WHITE;

    private static final String DATE_PDF =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constantes.FORMAT_DATETIME));

    private PDFExporter() {}

    // ══════════════════════════════════════════════════════════════════════════
    //  1. LISTE DES ÉTUDIANTS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Exporte une liste d'étudiants en tableau PDF.
     *
     * @param etudiants liste à exporter
     * @param dest      fichier de destination (.pdf)
     */
    public static void exportListeEtudiants(List<Etudiant> etudiants, File dest) {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 50, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(dest));
            writer.setPageEvent(new PiedDePage("Liste des Étudiants"));
            doc.open();

            entete(doc, "Liste des Étudiants",
                    etudiants.size() + " étudiant(s)  —  Généré le " + DATE_PDF);

            float[] cols = {1.2f, 2f, 2f, 2.5f, 1.2f};
            PdfPTable table = creerTableau(cols);
            enteteTableau(table, "CNE", "Nom", "Prénom", "Email", "Statut");

            boolean paire = false;
            for (Etudiant e : etudiants) {
                BaseColor fond = paire ? C_GRIS_CLAIR : C_BLANC;
                BaseColor cStatut = Constantes.STATUT_ACTIF.equals(e.getStatut().name())
                                    ? C_VERT : C_ROUGE;
                cellule(table, e.getCne(),       fond, Element.ALIGN_LEFT);
                cellule(table, e.getNom(),       fond, Element.ALIGN_LEFT);
                cellule(table, e.getPrenom(),    fond, Element.ALIGN_LEFT);
                cellule(table, nvl(e.getEmail()), fond, Element.ALIGN_LEFT);
                celluleColoree(table, e.getStatut().name(), fond, cStatut);
                paire = !paire;
            }
            doc.add(table);
            doc.close();

        } catch (DocumentException | IOException ex) {
            throw new RuntimeException("Erreur export liste étudiants PDF", ex);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  2. RELEVÉ DE NOTES INDIVIDUEL
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Exporte le relevé de notes d'un étudiant.
     *
     * @param etudiant étudiant concerné
     * @param notes    liste de ses notes
     * @param dest     fichier de destination
     */
    public static void exportReleveNotes(Etudiant etudiant, List<Note> notes, File dest) {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 50, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(dest));
            writer.setPageEvent(new PiedDePage("Relevé de Notes"));
            doc.open();

            entete(doc, "Relevé de Notes", etudiant.getNomComplet() + "  |  CNE : " + etudiant.getCne());

            // Calcul moyenne pondérée simple (sans coeff ici — affichage brut)
            double somme = notes.stream().mapToDouble(Note::getValeur).sum();
            double moy   = notes.isEmpty() ? 0 : Math.round(somme / notes.size() * 100.0) / 100.0;
            String mention = getMention(moy);

            doc.add(paragraphe(
                "Moyenne : " + moy + " / 20   |   Mention : " + mention,
                FontFactory.HELVETICA_BOLD, 12, C_ACCENT));
            doc.add(Chunk.NEWLINE);

            float[] cols = {2.5f, 1.5f, 1.5f, 1.5f};
            PdfPTable table = creerTableau(cols);
            enteteTableau(table, "Sous-module", "Type", "Note / 20", "Date saisie");

            boolean paire = false;
            for (Note n : notes) {
                BaseColor fond   = paire ? C_GRIS_CLAIR : C_BLANC;
                BaseColor cNote  = n.getValeur() >= Constantes.MOYENNE_REUSSITE ? C_VERT : C_ROUGE;
                cellule(table, nvl(n.getSousModuleIntitule()), fond, Element.ALIGN_LEFT);
                cellule(table, n.getTypeNote().name(),         fond, Element.ALIGN_CENTER);
                celluleColoree(table, String.format("%.2f", n.getValeur()), fond, cNote);
                cellule(table, n.getDateSaisie() != null
                               ? n.getDateSaisie().format(
                                   DateTimeFormatter.ofPattern(Constantes.FORMAT_DATE)) : "—",
                        fond, Element.ALIGN_CENTER);
                paire = !paire;
            }
            doc.add(table);
            doc.close();

        } catch (DocumentException | IOException ex) {
            throw new RuntimeException("Erreur export relevé de notes PDF", ex);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  3. STATISTIQUES FILIÈRE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Exporte un rapport statistique de filière.
     *
     * @param data clés attendues : filiere, taux_reussite, total_etudiants,
     *             meilleure_moy, classement (List<Map<String,Object>>)
     * @param dest fichier de destination
     */
    public static void exportStatistiquesFiliere(Map<String, Object> data, File dest) {
        try {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(dest));
            writer.setPageEvent(new PiedDePage("Statistiques Filière"));
            doc.open();

            String filiere = str(data, "filiere");
            entete(doc, "Statistiques — Filière " + filiere,
                    "Généré le " + DATE_PDF);

            // Bloc résumé KPI
            kpi(doc, new String[][]{
                {"Total étudiants",  str(data, "total_etudiants")},
                {"Taux de réussite", str(data, "taux_reussite") + " %"},
                {"Meilleure moy.",   str(data, "meilleure_moy") + " / 20"}
            });

            doc.add(Chunk.NEWLINE);

            // Tableau classement
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> classement =
                    (List<Map<String, Object>>) data.get("classement");

            if (classement != null && !classement.isEmpty()) {
                PdfPTable table = creerTableau(new float[]{0.5f, 2.5f, 1.2f, 1.5f, 1.2f});
                enteteTableau(table, "Rang", "Étudiant", "Moyenne", "Mention", "Statut");

                boolean paire = false;
                for (Map<String, Object> row : classement) {
                    if ("RESUME".equals(row.get("type"))) continue;
                    BaseColor fond    = paire ? C_GRIS_CLAIR : C_BLANC;
                    boolean admis     = "ADMIS".equals(str(row, "statut"));
                    cellule(table, str(row, "rang"),             fond, Element.ALIGN_CENTER);
                    cellule(table, str(row, "etudiant"),         fond, Element.ALIGN_LEFT);
                    cellule(table, str(row, "moyenne_generale"), fond, Element.ALIGN_CENTER);
                    cellule(table, str(row, "mention"),          fond, Element.ALIGN_CENTER);
                    celluleColoree(table, str(row, "statut"),    fond, admis ? C_VERT : C_ROUGE);
                    paire = !paire;
                }
                doc.add(table);
            }
            doc.close();

        } catch (DocumentException | IOException ex) {
            throw new RuntimeException("Erreur export statistiques filière PDF", ex);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  4. RAPPORT PROMOTION (appelé depuis les controllers)
    // ══════════════════════════════════════════════════════════════════════════

    public static void exporterRapportPromotion(List<Map<String, Object>> rapport,
                                                Promotion promotion, File dest) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("filiere",           promotion != null ? promotion.getIntitule() : "—");
        data.put("total_etudiants",   rapport.size());
        double moyMax = rapport.stream()
                .filter(r -> !"RESUME".equals(r.get("type")))
                .mapToDouble(r -> toDouble(r.get("moyenne_generale")))
                .max().orElse(0.0);
        data.put("meilleure_moy",     String.format("%.2f", moyMax));
        long admis = rapport.stream()
                .filter(r -> "ADMIS".equals(r.get("statut"))).count();
        data.put("taux_reussite",     rapport.isEmpty() ? "0"
                : String.format("%.1f", (double) admis / rapport.size() * 100));
        data.put("classement", rapport);
        exportStatistiquesFiliere(data, dest);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  5. BULLETIN INDIVIDUEL
    // ══════════════════════════════════════════════════════════════════════════

    public static void exporterBulletinEtudiant(Map<String, Object> fiche, File dest) {
        try {
            Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(dest));
            writer.setPageEvent(new PiedDePage("Bulletin de Notes"));
            doc.open();

            entete(doc, "Bulletin de Notes", str(fiche, "nom_complet"));

            doc.add(paragraphe("CNE : " + str(fiche, "cne"),
                    FontFactory.HELVETICA, 10, C_GRIS_TEXTE));
            doc.add(paragraphe(
                "Moyenne générale : " + str(fiche, "moyenne_generale") + " / 20" +
                "   |   " + str(fiche, "mention") +
                "   |   " + str(fiche, "statut"),
                FontFactory.HELVETICA_BOLD, 12, C_ACCENT));
            doc.add(Chunk.NEWLINE);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> details =
                    (List<Map<String, Object>>) fiche.get("detail_notes");

            if (details != null && !details.isEmpty()) {
                PdfPTable table = creerTableau(new float[]{2f, 2f, 1.2f, 1f, 1.2f});
                enteteTableau(table, "Module", "Sous-module", "Type", "Note", "Moy. SM");
                boolean paire = false;
                for (Map<String, Object> row : details) {
                    BaseColor fond = paire ? C_GRIS_CLAIR : C_BLANC;
                    double val = toDouble(row.get("valeur"));
                    cellule(table, str(row, "module"),      fond, Element.ALIGN_LEFT);
                    cellule(table, str(row, "sous_module"), fond, Element.ALIGN_LEFT);
                    cellule(table, str(row, "type_note"),   fond, Element.ALIGN_CENTER);
                    celluleColoree(table, str(row, "valeur"), fond,
                            val >= Constantes.MOYENNE_REUSSITE ? C_VERT : C_ROUGE);
                    cellule(table, str(row, "moyenne_sm"),  fond, Element.ALIGN_CENTER);
                    paire = !paire;
                }
                doc.add(table);
            }
            doc.close();

        } catch (DocumentException | IOException ex) {
            throw new RuntimeException("Erreur export bulletin PDF", ex);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS PRIVÉS
    // ══════════════════════════════════════════════════════════════════════════

    private static void entete(Document doc, String titre, String sousTitre)
            throws DocumentException {
        Font fTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, C_BLANC);
        Font fSous  = FontFactory.getFont(FontFactory.HELVETICA, 11, C_BLANC);

        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        header.setSpacingAfter(12);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(C_ENTETE);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(14);

        Paragraph p = new Paragraph();
        p.add(new Chunk(AppConfig.get().getAppName() + "\n", fSous));
        p.add(new Chunk(titre + "\n", fTitre));
        p.add(new Chunk(sousTitre, fSous));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        header.addCell(cell);
        doc.add(header);
    }

    private static void kpi(Document doc, String[][] data) throws DocumentException {
        PdfPTable t = new PdfPTable(data.length);
        t.setWidthPercentage(80);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setSpacingAfter(10);

        Font fLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, C_GRIS_TEXTE);
        Font fVal   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, C_ENTETE);

        for (String[] d : data) {
            PdfPCell c = new PdfPCell();
            c.setBorderColor(C_GRIS_CLAIR);
            c.setPadding(10);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.addElement(new Paragraph(d[0], fLabel));
            c.addElement(new Paragraph(d[1], fVal));
            t.addCell(c);
        }
        doc.add(t);
    }

    private static PdfPTable creerTableau(float[] largeurs) throws DocumentException {
        PdfPTable t = new PdfPTable(largeurs);
        t.setWidthPercentage(100);
        t.setSpacingBefore(6);
        return t;
    }

    private static void enteteTableau(PdfPTable table, String... colonnes) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, C_BLANC);
        for (String col : colonnes) {
            PdfPCell c = new PdfPCell(new Phrase(col, f));
            c.setBackgroundColor(C_ACCENT);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(7);
            c.setBorderColor(C_BLANC);
            table.addCell(c);
        }
    }

    private static void cellule(PdfPTable t, String texte, BaseColor fond, int align) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA, 9, C_GRIS_TEXTE);
        PdfPCell c = new PdfPCell(new Phrase(nvl(texte), f));
        c.setBackgroundColor(fond);
        c.setHorizontalAlignment(align);
        c.setPadding(5);
        c.setBorderColor(C_GRIS_CLAIR);
        t.addCell(c);
    }

    private static void celluleColoree(PdfPTable t, String texte,
                                       BaseColor fond, BaseColor couleurTexte) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, couleurTexte);
        PdfPCell c = new PdfPCell(new Phrase(nvl(texte), f));
        c.setBackgroundColor(fond);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(5);
        c.setBorderColor(C_GRIS_CLAIR);
        t.addCell(c);
    }

    private static Paragraph paragraphe(String texte, String police, float taille, BaseColor couleur) {
        return new Paragraph(texte, FontFactory.getFont(police, taille, couleur));
    }

    private static String getMention(double moy) {
        if (moy >= Constantes.MENTION_TRES_BIEN)  return "Très Bien";
        if (moy >= Constantes.MENTION_BIEN)        return "Bien";
        if (moy >= Constantes.MENTION_ASSEZ_BIEN)  return "Assez Bien";
        if (moy >= Constantes.MENTION_PASSABLE)    return "Passable";
        return "Insuffisant";
    }

    private static String str(Map<String, Object> row, String key) {
        Object v = row == null ? null : row.get(key);
        return v != null ? v.toString() : "—";
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        try { return Double.parseDouble(o.toString()); }
        catch (Exception e) { return 0.0; }
    }

    private static String nvl(String s) { return s != null ? s : "—"; }

    // ── Pied de page ─────────────────────────────────────────────────────────
    private static class PiedDePage extends PdfPageEventHelper {
        private final String titre;
        private final Font font =
                FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);

        PiedDePage(String titre) { this.titre = titre; }

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContent();
            String texte = titre + "  |  Généré le " + DATE_PDF +
                           "  |  Page " + writer.getPageNumber();
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(texte, font),
                    (doc.right() - doc.left()) / 2 + doc.leftMargin(),
                    doc.bottom() - 10, 0);
        }
    }
}
