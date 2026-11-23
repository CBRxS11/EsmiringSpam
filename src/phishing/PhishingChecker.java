package phishing;

import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based phishing indicator checker.
 *
 * This class does not decide whether an email is spam or ham. It independently
 * reports suspicious phrases and URL characteristics for the application to show.
 */
public class PhishingChecker {

    /*
     * Explicit links (http/https/www) and bare domain-like links are both checked.
     * The negative lookbehinds help avoid treating ordinary email addresses as URLs.
     */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(?<![a-z0-9._%+@/-])(?:https?://|www\\.)[^\\s<>\"']+" +
            "|(?<![a-z0-9._%+@/:/-])(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+" +
            "[a-z]{2,}(?::\\d{1,5})?(?:[/#?][^\\s<>\"']*)?"
    );

    // Kept consistent with the research document's example of unusually dotted hosts.
    private static final int MAX_HOST_DOTS = 3;

    private static final List<String> DEFAULT_SUSPICIOUS_PHRASES = List.of(
            "verify your account",
            "confirm your password",
            "reset your password",
            "confirm your identity",
            "account suspended",
            "account will be locked",
            "unusual sign in activity",
            "update your payment information",
            "click here to verify",
            "your account has been limited",
            // Kept because the research document uses "urgent" as an example.
            // This broad word is a weak indicator and can produce false positives.
            "urgent"
    );

    private final Trie suspiciousPhrases;

    public PhishingChecker() {
        this(DEFAULT_SUSPICIOUS_PHRASES);
    }

    /** Allows the phrase list to be configured without changing the detector. */
    public PhishingChecker(List<String> configuredPhrases) {
        suspiciousPhrases = new Trie();
        if (configuredPhrases != null) {
            for (String phrase : configuredPhrases) {
                suspiciousPhrases.insert(phrase);
            }
        }
    }

    /** Checks the raw, original email text and returns all detected indicators. */
    public PhishingReport check(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return new PhishingReport(List.of(), List.of(), List.of());
        }

        List<String> phraseMatches = new ArrayList<>(
                suspiciousPhrases.findMatches(rawEmail)
        );

        // Preserve insertion order and avoid inspecting the same link twice.
        Set<String> urls = new LinkedHashSet<>(extractUrls(rawEmail));
        List<String> inspectedUrls = new ArrayList<>(urls);
        List<String> urlWarnings = new ArrayList<>();

        for (String url : urls) {
            urlWarnings.addAll(checkUrl(url));
        }

        return new PhishingReport(phraseMatches, urlWarnings, inspectedUrls);
    }

    /** Extracts explicit URLs and bare domain-like links from the email. */
    List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return urls;
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = trimTrailingPunctuation(matcher.group());
            if (!candidate.isBlank()) {
                urls.add(candidate);
            }
        }
        return urls;
    }

    /** Removes sentence punctuation without removing balanced parentheses in a URL path. */
    private String trimTrailingPunctuation(String value) {
        String result = value;
        while (!result.isEmpty() && ".,;:!?".indexOf(result.charAt(result.length() - 1)) >= 0) {
            result = result.substring(0, result.length() - 1);
        }

        while (result.endsWith(")") && countCharacter(result, ')') > countCharacter(result, '(')) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private int countCharacter(String value, char target) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    /** Applies the configured structural URL checks. */
    private List<String> checkUrl(String extractedUrl) {
        List<String> warnings = new ArrayList<>();
        String normalizedUrl = addSchemeIfMissing(extractedUrl);

        final URI uri;
        try {
            uri = new URI(normalizedUrl);
        } catch (URISyntaxException ex) {
            // A malformed candidate is not automatically called phishing.
            return warnings;
        }

        String authority = uri.getRawAuthority();
        if (authority == null || authority.isBlank()) {
            return warnings;
        }

        // An @ in the authority can make the part before it look like the hostname.
        if (authority.indexOf('@') >= 0) {
            warnings.add("At-sign in URL authority: " + extractedUrl);
        }

        String host = extractHost(uri, authority);
        if (host == null || host.isBlank()) {
            return warnings;
        }

        host = normalizeHost(host);
        if (host == null || host.isBlank()) {
            return warnings;
        }

        if (isIpAddress(host)) {
            warnings.add("IP address used as hostname: " + extractedUrl);
        }

        int dotCount = countDots(host);
        if (dotCount > MAX_HOST_DOTS) {
            warnings.add("Hostname has more than " + MAX_HOST_DOTS
                    + " dots: " + extractedUrl);
        }

        return warnings;
    }

    private String addSchemeIfMissing(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return url;
        }
        return "https://" + url;
    }

    /**
     * Gets the host from URI. The authority fallback supports Unicode/IDN hostnames,
     * for which URI.getHost() may return null.
     */
    private String extractHost(URI uri, String authority) {
        String host = uri.getHost();
        if (host != null && !host.isBlank()) {
            return host;
        }

        String hostPort = authority;
        int atIndex = hostPort.lastIndexOf('@');
        if (atIndex >= 0) {
            hostPort = hostPort.substring(atIndex + 1);
        }

        if (hostPort.startsWith("[")) {
            int closingBracket = hostPort.indexOf(']');
            if (closingBracket > 0) {
                return hostPort.substring(1, closingBracket);
            }
            return null;
        }

        // Remove a port only when the suffix is numeric.
        int colonIndex = hostPort.lastIndexOf(':');
        if (colonIndex >= 0 && hostPort.indexOf(':') == colonIndex) {
            String port = hostPort.substring(colonIndex + 1);
            if (port.isEmpty() || port.matches("\\d+")) {
                hostPort = hostPort.substring(0, colonIndex);
            }
        }

        return hostPort;
    }

    private String normalizeHost(String host) {
        String value = host.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        while (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }

        // IPv6 literals contain colons and must not be passed through IDN conversion.
        if (value.indexOf(':') >= 0) {
            return value.toLowerCase(Locale.ROOT);
        }

        try {
            return IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isIpAddress(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }

        // URI parsing validates bracketed IPv6 syntax; a colon identifies an IPv6 literal here.
        if (host.indexOf(':') >= 0) {
            return host.matches("[0-9a-fA-F:.]+");
        }

        String[] octets = host.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }

        for (String octet : octets) {
            if (!octet.matches("\\d{1,3}")) {
                return false;
            }
            int value = Integer.parseInt(octet);
            if (value > 255) {
                return false;
            }
        }
        return true;
    }

    private int countDots(String host) {
        int count = 0;
        for (int i = 0; i < host.length(); i++) {
            if (host.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }
}