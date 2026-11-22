import classifier.Classifier;
import classifier.KNN;
import classifier.LogisticRegression;
import classifier.NaiveBayes;
import data.DataLoader;
import data.DataSplitter;
import data.Preprocessor;
import eval.Evaluator;
import eval.Evaluator.EvaluationResult;
import java.util.ArrayList;
import java.util.List;
import model.Email;

/**
 * Main: loads the data, splits it, trains Naive Bayes, and prints a quick accuracy.
 * (The Evaluator, coming next, will replace the quick accuracy check below.)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "data/emails.csv";

        // 1. Load  (if your columns are text,spam use: loader.loadEmails(path, 1, 0))
        DataLoader loader = new DataLoader();
        List<Email> emails = loader.loadEmails(path, 1, 0);

        int removed = new data.Deduplicator().removeDuplicates(emails);
        System.out.println("Duplicates removed: " + removed);
        
        // 2. Clean
        new Preprocessor().cleanAll(emails);

        int spamCount = 0;
        for (Email e : emails) if (e.isSpam()) spamCount++;
        System.out.println("Total emails: " + emails.size());
        System.out.println("Spam: " + spamCount + "  Ham: " + (emails.size() - spamCount));
        System.out.printf("Spam ratio: %.1f%%%n", 100.0 * spamCount / emails.size());

        // 3. Split 80% train / 20% test, fixed seed 42
        DataSplitter split = new DataSplitter(emails, 0.8, 42);
        System.out.println("Train: " + split.getTrain().size()
                + "  Test: " + split.getTest().size());

        // 4. Evaluate classifiers
        Evaluator evaluator = new Evaluator();
        List<EvaluationResult> results = new ArrayList<>();

        Classifier nb = new NaiveBayes();
        EvaluationResult nbResult = evaluator.trainAndEvaluate(nb, split.getTrain(), split.getTest());
        evaluator.printReport(nbResult);
        results.add(nbResult);

        // k-Nearest Neighbors
        Classifier knn = new KNN(5);

        EvaluationResult knnResult = evaluator.trainAndEvaluate(
                knn, split.getTrain(), split.getTest()
        );

        evaluator.printReport(knnResult);
        results.add(knnResult);

        // Logistic Regression
        Classifier lr = new LogisticRegression();

        EvaluationResult lrResult = evaluator.trainAndEvaluate(
                lr, split.getTrain(), split.getTest()
        );

        evaluator.printReport(lrResult);
        results.add(lrResult);

        // 5. Export results for Excel / Google Sheets charts
        evaluator.exportToCsv(results, "results.csv");
    }
}

