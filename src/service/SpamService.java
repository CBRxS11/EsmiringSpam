package service;

import classifier.Classifier;
import classifier.KNN;
import classifier.LogisticRegression;
import classifier.NaiveBayes;
import data.DataLoader;
import data.DataSplitter;
import data.Deduplicator;
import data.Preprocessor;
import model.Email;
import phishing.PhishingChecker;
import phishing.PhishingReport;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SpamService: the one class the Email Checker window talks to.
 *
 * It loads the dataset, trains Naive Bayes, kNN and Logistic Regression ONCE
 * (same cleaning, 80/20 split and seed 42 as Main), and then answers
 * "is this email spam?" for any pasted text. It also runs the phishing checker.
 *
 * Training takes a few seconds, so callers should run initialize() on a
 * background thread (the window uses a SwingWorker).
 */
public class SpamService {

    private static final double TRAIN_RATIO = 0.8;
    private static final long RANDOM_SEED = 42L;

    private final Preprocessor preprocessor = new Preprocessor();
    private final PhishingChecker phishingChecker = new PhishingChecker();

    // LinkedHashMap keeps the order: Naive Bayes, kNN, Logistic Regression.
    private final Map<String, Classifier> classifiers = new LinkedHashMap<>();
    private boolean ready = false;

    /** Loads data/emails.csv and trains all three classifiers. Slow: call off the UI thread. */
    public synchronized void initialize(String csvPath) throws IOException {
        List<Email> emails = new DataLoader().loadEmails(csvPath, 1, 0);
        if (emails.isEmpty()) {
            throw new IllegalStateException("No emails were loaded from " + csvPath);
        }
        new Deduplicator().removeDuplicates(emails);
        preprocessor.cleanAll(emails);

        DataSplitter split = new DataSplitter(emails, TRAIN_RATIO, RANDOM_SEED);

        classifiers.clear();
        Classifier[] all = { new NaiveBayes(), new KNN(5), new LogisticRegression() };
        for (Classifier classifier : all) {
            classifier.train(split.getTrain());
            classifiers.put(classifier.getName(), classifier);
        }
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }

    /** Names for the dropdown, in a fixed order. */
    public List<String> getClassifierNames() {
        return List.copyOf(classifiers.keySet());
    }

    /** true = spam, false = ham, according to the chosen classifier. */
    public boolean isSpam(String classifierName, String rawText) {
        Classifier classifier = classifiers.get(classifierName);
        if (classifier == null) {
            throw new IllegalArgumentException("Unknown classifier: " + classifierName);
        }
        // The label (false) is unknown for a new email and is never used for prediction.
        Email email = new Email(rawText, false);
        email.setTokens(preprocessor.clean(rawText));
        return classifier.predict(email);
    }

    /** Runs the rule-based phishing checker on the raw, original text. */
    public PhishingReport checkPhishing(String rawText) {
        return phishingChecker.check(rawText);
    }
}