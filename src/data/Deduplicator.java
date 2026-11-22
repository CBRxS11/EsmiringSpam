package data;

import model.Email;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Removes emails whose text already appeared earlier in the list. */
public class Deduplicator {

    /** Returns how many duplicates were removed. */
    public int removeDuplicates(List<Email> emails) {
        Set<String> seen = new HashSet<>();          // HashSet: O(1) "have I seen this?"
        int before = emails.size();
        emails.removeIf(e -> !seen.add(e.getRawText().trim().toLowerCase()));
        return before - emails.size();
    }
}