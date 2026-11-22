package data;

import model.Email;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * DataLoader: reads a CSV file and turns each row into an Email object.
 *
 * Expected format (the first row is always treated as a header and skipped.):
 *     label,text
 *     ham,"Hi, are we still meeting tomorrow?"
 *     spam,"WINNER!! Claim your free prize now"
 *
 * By default column 0 is the label and column 1 is the text.
 * If your dataset is different (for example text first, label second),
 * use loadEmails(path, labelColumn, textColumn).
 *
 * A label counts as spam if it is "spam" or "1". Everything else is ham.
 */
public class DataLoader {

    /** Loads a dataset where column 0 = label and column 1 = text. */
    public List<Email> loadEmails(String filePath) throws IOException {
        return loadEmails(filePath, 0, 1);
    }

    public List<Email> loadEmails(String filePath, int labelColumn, int textColumn)
            throws IOException {

        String content = new String(Files.readAllBytes(Paths.get(filePath)),
                StandardCharsets.UTF_8);
        List<List<String>> rows = parseCsv(content);

        List<Email> emails = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {   // start at 1: row 0 is the header
            List<String> row = rows.get(r);
            if (row.size() <= Math.max(labelColumn, textColumn)) continue; // broken row

            String label = row.get(labelColumn).trim().toLowerCase();
            String text = row.get(textColumn).trim();

            if (!isKnownLabel(label)) continue;   // skips the header row and junk
            if (text.isEmpty()) continue;         // skips empty emails

            boolean isSpam = label.equals("spam") || label.equals("1");
            emails.add(new Email(text, isSpam));
        }
        return emails;
    }

    private boolean isKnownLabel(String label) {
        return label.equals("spam") || label.equals("ham")
                || label.equals("1") || label.equals("0");
    }

    /**
     * A small CSV reader. We write our own because a simple split(",")
     * breaks when an email itself contains commas or line breaks inside quotes.
     */
    private List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // Two quotes in a row ("") means one real quote character
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else if (c != '\r') {
                field.append(c);
            }
        }

        // Last line of the file may not end with a line break
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
