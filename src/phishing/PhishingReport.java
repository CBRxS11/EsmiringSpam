package phishing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Immutable result returned by PhishingChecker. */
public final class PhishingReport {

    private final List<String> phraseMatches;
    private final List<String> urlWarnings;
    private final List<String> inspectedUrls;

    /** Backwards-compatible constructor for callers using the original two lists. */
    public PhishingReport(List<String> phraseMatches, List<String> urlWarnings) {
        this(phraseMatches, urlWarnings, List.of());
    }

    public PhishingReport(
            List<String> phraseMatches,
            List<String> urlWarnings,
            List<String> inspectedUrls) {

        this.phraseMatches = immutableDistinct(phraseMatches);
        this.urlWarnings = immutableDistinct(urlWarnings);
        this.inspectedUrls = immutableDistinct(inspectedUrls);
    }

    public boolean hasIndicators() {
        return !phraseMatches.isEmpty() || !urlWarnings.isEmpty();
    }

    public List<String> getPhraseMatches() {
        return phraseMatches;
    }

    public List<String> getUrlWarnings() {
        return urlWarnings;
    }

    /** URLs the checker successfully extracted and attempted to inspect. */
    public List<String> getInspectedUrls() {
        return inspectedUrls;
    }

    public int getIndicatorCount() {
        return phraseMatches.size() + urlWarnings.size();
    }

    private static List<String> immutableDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> distinct = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                distinct.add(value);
            }
        }
        return List.copyOf(new ArrayList<>(distinct));
    }

    @Override
    public String toString() {
        return "PhishingReport{" +
                "phraseMatches=" + phraseMatches +
                ", urlWarnings=" + urlWarnings +
                ", inspectedUrls=" + inspectedUrls +
                '}';
    }
}