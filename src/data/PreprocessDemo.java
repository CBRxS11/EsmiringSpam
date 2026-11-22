package data;

import model.Email;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Prints proof of preprocessing (before/after, averages, top words) and checks the split. */
public class PreprocessDemo {

    public static void show(List<Email> all, List<Email> train, List<Email> test) {
        System.out.println("================ PREPROCESSING DEMO ================");

        // 1. Before / after for one spam email and one ham email
        Email firstSpam = null, firstHam = null;
        for (Email e : all) {
            if (e.isSpam() && firstSpam == null) firstSpam = e;
            if (!e.isSpam() && firstHam == null) firstHam = e;
        }
        printBeforeAfter("SPAM example", firstSpam);
        printBeforeAfter("HAM example", firstHam);

        // 2. Average tokens per email AFTER cleaning
        long total = 0, spamTotal = 0, hamTotal = 0;
        int spamN = 0, hamN = 0;
        for (Email e : all) {
            int n = e.getTokens().size();
            total += n;
            if (e.isSpam()) { spamTotal += n; spamN++; } else { hamTotal += n; hamN++; }
        }
        System.out.printf("Average tokens per email after cleaning: %.0f (spam %.0f, ham %.0f)%n",
                (double) total / all.size(), (double) spamTotal / spamN, (double) hamTotal / hamN);

        // 3. Vocabulary size = how many DIFFERENT words appear in the training emails
        Set<String> vocab = new HashSet<>();
        for (Email e : train) vocab.addAll(e.getTokens());
        System.out.println("Vocabulary size (distinct words in training data): " + vocab.size());
        System.out.println("(kNN and Logistic Regression keep only the top 3000 of these.)");

        // 4. Top 10 words in spam and in ham
        System.out.println("Top 10 words in SPAM: " + topWords(all, true, 10));
        System.out.println("Top 10 words in HAM : " + topWords(all, false, 10));

        // 5. Spam share in the train and test parts
        printSplit("Train", train);
        printSplit("Test ", test);
        System.out.println("====================================================\n");
    }

    private static void printBeforeAfter(String title, Email e) {
        String raw = e.getRawText().replace("\n", " ");
        System.out.println("---- " + title + " ----");
        System.out.println("BEFORE: " + (raw.length() > 150 ? raw.substring(0, 150) + "..." : raw));
        List<String> t = e.getTokens();
        System.out.println("AFTER : " + t.subList(0, Math.min(15, t.size())) + "  (" + t.size() + " tokens in total)");
        System.out.println();
    }

    /** Counts every word in spam (or ham) emails and returns the most common ones. */
    private static List<String> topWords(List<Email> emails, boolean spam, int count) {
        Map<String, Integer> freq = new HashMap<>();
        for (Email e : emails) {
            if (e.isSpam() != spam) continue;
            for (String w : e.getTokens()) freq.merge(w, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(freq.entrySet());
        entries.sort((a, b) -> b.getValue() - a.getValue());
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count && i < entries.size(); i++) {
            result.add(entries.get(i).getKey() + " (" + entries.get(i).getValue() + ")");
        }
        return result;
    }

    private static void printSplit(String name, List<Email> part) {
        int spam = 0;
        for (Email e : part) if (e.isSpam()) spam++;
        System.out.printf("%s: %d emails, %d spam (%.1f%%), %d ham%n",
                name, part.size(), spam, 100.0 * spam / part.size(), part.size() - spam);
    }
}