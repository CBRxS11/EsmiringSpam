package phishing;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Trie for case-insensitive matching of configured suspicious words and phrases.
 * Matching is performed on normalized text and respects complete word boundaries.
 */
public class Trie {

    private static final class TrieNode {
        private final Map<Character, TrieNode> children = new HashMap<>();
        private String terminalPhrase;
    }

    private final TrieNode root = new TrieNode();
    private int phraseCount;

    /** Inserts one word or phrase. Empty or punctuation-only values are ignored. */
    public void insert(String phrase) {
        String normalized = normalize(phrase);
        if (normalized.isEmpty()) {
            return;
        }

        TrieNode current = root;
        for (char character : normalized.toCharArray()) {
            current = current.children.computeIfAbsent(character, ignored -> new TrieNode());
        }

        if (current.terminalPhrase == null) {
            phraseCount++;
        }
        current.terminalPhrase = normalized;
    }

    /**
     * Returns each configured phrase found in the text once, in order of its
     * first occurrence. Punctuation is treated as a word separator.
     */
    public Set<String> findMatches(String text) {
        Set<String> matches = new LinkedHashSet<>();
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return matches;
        }

        for (int start = 0; start < normalized.length(); start++) {
            // A phrase may only begin at the start of text or after a separator.
            if (start > 0 && normalized.charAt(start - 1) != ' ') {
                continue;
            }

            TrieNode current = root;
            for (int index = start; index < normalized.length(); index++) {
                current = current.children.get(normalized.charAt(index));
                if (current == null) {
                    break;
                }

                boolean endOfText = index == normalized.length() - 1;
                boolean endOfWord = !endOfText && normalized.charAt(index + 1) == ' ';
                if (current.terminalPhrase != null && (endOfText || endOfWord)) {
                    matches.add(current.terminalPhrase);
                }
            }
        }

        return matches;
    }

    /** Returns the number of distinct phrases stored in the Trie. */
    public int size() {
        return phraseCount;
    }

    private String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}