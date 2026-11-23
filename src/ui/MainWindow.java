package ui;

import classifier.Classifier;
import classifier.KNN;
import classifier.LogisticRegression;
import classifier.NaiveBayes;
import data.DataLoader;
import data.DataSplitter;
import data.Deduplicator;
import data.Preprocessor;
import eval.Evaluator;
import eval.Evaluator.EvaluationResult;
import model.Email;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dataset evaluation window for EsmiringSpam.
 *
 * On startup, this window loads data/emails.csv, removes duplicates,
 * preprocesses the emails, creates the documented 80/20 split (seed 42),
 * and evaluates all three classifiers on the same test set.
 *
 * The existing data, model, classifier, and evaluation classes are used as-is.
 */
public class MainWindow extends JFrame {

    private static final String DATA_PATH = "data/emails.csv";
    private static final double TRAIN_RATIO = 0.8;
    private static final long RANDOM_SEED = 42L;

    private final JLabel statusLabel = new JLabel("Loading dataset and evaluating classifiers...");
    private final JLabel datasetSummaryLabel = new JLabel("Dataset information will appear here.");
    private final JLabel classSummaryLabel = new JLabel(" ");
    private final JLabel bestResultLabel = new JLabel(" ");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Algorithm", "Accuracy", "Precision", "Recall", "F1-score"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable resultsTable = new JTable(tableModel);
    private final JButton detailsButton = new JButton("Show details");
    private final JPanel detailsPanel = new JPanel(new BorderLayout(8, 8));
    private final javax.swing.JTextArea detailsText = new javax.swing.JTextArea();

    private boolean detailsVisible;

    private MainWindow() {
        super("EsmiringSpam - Dataset Summary");
        buildWindow();
        connectActions();
    }

    private void buildWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 560));
        setSize(1000, 680);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("EsmiringSpam");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));

        JPanel header = new JPanel(new BorderLayout(4, 4));
        header.add(title, BorderLayout.NORTH);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        header.add(statusLabel, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));

        content.add(createDatasetPanel());
        content.add(javax.swing.Box.createVerticalStrut(12));
        content.add(createResultsPanel());
        content.add(javax.swing.Box.createVerticalStrut(8));

        detailsPanel.setBorder(BorderFactory.createTitledBorder("Evaluation Details"));
        detailsText.setEditable(false);
        detailsText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        detailsText.setLineWrap(false);
        detailsText.setText("Detailed results will appear here.");
        detailsPanel.add(new JScrollPane(detailsText), BorderLayout.CENTER);
        detailsPanel.setPreferredSize(new Dimension(900, 340));
        detailsPanel.setVisible(false);
        detailsButton.setEnabled(false);
        content.add(detailsPanel);

        JScrollPane contentScroll = new JScrollPane(content);
        contentScroll.setBorder(BorderFactory.createEmptyBorder());
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(contentScroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.add(bestResultLabel, BorderLayout.CENTER);
        footer.add(detailsButton, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Dataset Summary", root);
        tabs.addTab("Email Checker", new EmailCheckerPanel());
        setContentPane(tabs);
    }

    private JPanel createDatasetPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Dataset Summary"));

        datasetSummaryLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        classSummaryLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        panel.add(datasetSummaryLabel);
        panel.add(classSummaryLabel);
        return panel;
    }

    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Classifier Results (Test Set)"));

        resultsTable.setFillsViewportHeight(true);
        resultsTable.setRowHeight(28);
        resultsTable.setAutoCreateRowSorter(false);
        resultsTable.getTableHeader().setReorderingAllowed(false);
        resultsTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        resultsTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        panel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        JLabel phishingNote = new JLabel(
                "Phishing checks are separate. This dataset has no phishing labels or URLs, "
                        + "so phishing accuracy cannot be measured with it."
        );
        phishingNote.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        phishingNote.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(phishingNote, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(900, 190));
        return panel;
    }

    private void connectActions() {
        detailsButton.addActionListener(this::toggleDetails);
    }

    private void toggleDetails(ActionEvent event) {
        detailsVisible = !detailsVisible;
        detailsPanel.setVisible(detailsVisible);
        detailsButton.setText(detailsVisible ? "Hide details" : "Show details");
        revalidate();
        repaint();
    }

    /** Loads the supplied dataset and evaluates each classifier on the same test split. */
    private static AnalysisSummary analyzeDataset() throws IOException {
        DataLoader loader = new DataLoader();
        List<Email> emails = loader.loadEmails(DATA_PATH, 1, 0);

        if (emails.isEmpty()) {
            throw new IllegalStateException("No emails were loaded from " + DATA_PATH);
        }

        int removedDuplicates = new Deduplicator().removeDuplicates(emails);
        if (emails.isEmpty()) {
            throw new IllegalStateException("No emails remain after duplicate removal.");
        }

        int spamCount = 0;
        for (Email email : emails) {
            if (email.isSpam()) {
                spamCount++;
            }
        }
        int hamCount = emails.size() - spamCount;

        Preprocessor preprocessor = new Preprocessor();
        preprocessor.cleanAll(emails);

        DataSplitter splitter = new DataSplitter(emails, TRAIN_RATIO, RANDOM_SEED);
        List<Email> trainingData = splitter.getTrain();
        List<Email> testingData = splitter.getTest();

        if (trainingData.isEmpty() || testingData.isEmpty()) {
            throw new IllegalStateException("The training or testing split is empty.");
        }

        List<Classifier> classifiers = new ArrayList<>();
        classifiers.add(new NaiveBayes());
        classifiers.add(new KNN(5));
        classifiers.add(new LogisticRegression());

        Evaluator evaluator = new Evaluator();
        List<EvaluationResult> results = new ArrayList<>();

        for (Classifier classifier : classifiers) {
            results.add(evaluator.trainAndEvaluate(classifier, trainingData, testingData));
        }

        return new AnalysisSummary(
                emails.size(), spamCount, hamCount, removedDuplicates,
                trainingData.size(), testingData.size(), results
        );
    }

    private void startAnalysis() {
        SwingWorker<AnalysisSummary, Void> worker = new SwingWorker<>() {
            @Override
            protected AnalysisSummary doInBackground() throws Exception {
                return analyzeDataset();
            }

            @Override
            protected void done() {
                try {
                    AnalysisSummary summary = get();
                    displaySummary(summary);
                    statusLabel.setText("Analysis complete · " + DATA_PATH);
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null
                            ? exception : exception.getCause();
                    statusLabel.setText("Unable to analyze dataset.");
                    JOptionPane.showMessageDialog(
                            MainWindow.this,
                            "Unable to load or evaluate the dataset.\n\n"
                                    + "Run the application from the project root and make sure "
                                    + DATA_PATH + " exists.\n\n"
                                    + cause.getMessage(),
                            "Dataset Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void displaySummary(AnalysisSummary summary) {
        datasetSummaryLabel.setText(
                "Emails: " + summary.totalEmails
                        + "    |    Training: " + summary.trainingEmails
                        + "    |    Testing: " + summary.testingEmails
                        + "    |    Duplicates removed: " + summary.duplicatesRemoved
        );
        classSummaryLabel.setText(
                "Ham: " + summary.hamCount + "    |    Spam: " + summary.spamCount
        );

        tableModel.setRowCount(0);
        EvaluationResult bestResult = null;

        for (EvaluationResult result : summary.results) {
            tableModel.addRow(new Object[]{
                    result.classifierName,
                    formatPercent(result.accuracy),
                    formatPercent(result.precision),
                    formatPercent(result.recall),
                    formatPercent(result.f1Score)
            });

            if (bestResult == null || result.f1Score > bestResult.f1Score) {
                bestResult = result;
            }
        }

        if (bestResult != null) {
            bestResultLabel.setText(
                    "Highest F1-score: " + bestResult.classifierName
                            + " (" + formatPercent(bestResult.f1Score) + ")"
            );
        }

        detailsText.setText(buildDetails(summary));
        detailsText.setCaretPosition(0);
        detailsButton.setEnabled(true);
    }

    private String buildDetails(AnalysisSummary summary) {
        StringBuilder output = new StringBuilder();

        output.append("DATASET\n");
        output.append("-------\n");
        output.append("File: ").append(DATA_PATH).append('\n');
        output.append("Unique emails: ").append(summary.totalEmails).append('\n');
        output.append("Ham: ").append(summary.hamCount).append('\n');
        output.append("Spam: ").append(summary.spamCount).append('\n');
        output.append("Duplicates removed: ").append(summary.duplicatesRemoved).append('\n');
        output.append("Split: 80% training / 20% testing (seed 42)\n");
        output.append("Training emails: ").append(summary.trainingEmails).append('\n');
        output.append("Testing emails: ").append(summary.testingEmails).append("\n\n");

        for (EvaluationResult result : summary.results) {
            output.append(result.classifierName.toUpperCase(Locale.ROOT)).append('\n');
            output.append("-".repeat(result.classifierName.length())).append('\n');
            output.append("Accuracy: ").append(formatPercent(result.accuracy)).append('\n');
            output.append("Precision: ").append(formatPercent(result.precision)).append('\n');
            output.append("Recall: ").append(formatPercent(result.recall)).append('\n');
            output.append("F1-score: ").append(formatPercent(result.f1Score)).append('\n');
            output.append("Training time: ").append(result.trainTimeMs).append(" ms\n");
            output.append("Prediction time: ").append(result.predictTimeMs).append(" ms\n\n");

            output.append("Confusion Matrix (actual rows, predicted columns)\n");
            output.append("                         Predicted Spam   Predicted Ham\n");
            output.append(String.format(Locale.US,
                    "Actual Spam             %8d (TP)     %8d (FN)%n",
                    result.truePositives, result.falseNegatives));
            output.append(String.format(Locale.US,
                    "Actual Ham              %8d (FP)     %8d (TN)%n\n",
                    result.falsePositives, result.trueNegatives));
        }

        output.append("PHISHING LAYER\n");
        output.append("--------------\n");
        output.append("The phishing checker is a separate rule-based component. The current dataset\n");
        output.append("does not contain phishing labels or URLs, so phishing accuracy cannot be\n");
        output.append("calculated from this dataset.\n");

        return output.toString();
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.2f%%", value * 100.0);
    }

    private static final class AnalysisSummary {
        private final int totalEmails;
        private final int spamCount;
        private final int hamCount;
        private final int duplicatesRemoved;
        private final int trainingEmails;
        private final int testingEmails;
        private final List<EvaluationResult> results;

        private AnalysisSummary(
                int totalEmails,
                int spamCount,
                int hamCount,
                int duplicatesRemoved,
                int trainingEmails,
                int testingEmails,
                List<EvaluationResult> results) {
            this.totalEmails = totalEmails;
            this.spamCount = spamCount;
            this.hamCount = hamCount;
            this.duplicatesRemoved = duplicatesRemoved;
            this.trainingEmails = trainingEmails;
            this.testingEmails = testingEmails;
            this.results = List.copyOf(results);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
            window.startAnalysis();
        });
    }
}
