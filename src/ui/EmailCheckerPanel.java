package ui;

import phishing.PhishingReport;
import service.SpamService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * EmailCheckerPanel: the "paste an email" tab.
 *
 * The user pastes an email, picks an algorithm, and clicks Check. The panel shows
 * SPAM / NOT SPAM from the chosen classifier plus any phishing warnings.
 * All the work is done by SpamService; this class only draws the screen.
 */
public class EmailCheckerPanel extends JPanel {

    private static final String DATA_PATH = "data/emails.csv";

    private static final String SAMPLE_SPAM =
            "Subject: congratulations you have won\n\n"
            + "You have been selected to receive a free prize worth $1,000,000. "
            + "Click here to claim your money now. Limited time offer, act now!!!";

    private static final String SAMPLE_HAM =
            "Subject: project update\n\n"
            + "Hello team, please see the attached schedule for next week and let me know "
            + "if you have any questions. Regards, Paolo";

    private static final String SAMPLE_PHISHING =
            "Subject: security alert\n\n"
            + "Dear customer, we noticed unusual activity on your account. "
            + "Please verify your account today: http://192.168.10.5/secure@bank-login.example.com";

    private final SpamService service = new SpamService();

    private final JTextArea emailArea = new JTextArea(12, 60);
    private final JComboBox<String> algorithmBox = new JComboBox<>();
    private final JButton checkButton = new JButton("Check");
    private final JButton compareButton = new JButton("Compare all 3");
    private final JButton clearButton = new JButton("Clear");
    private final JButton spamSampleButton = new JButton("Sample: spam");
    private final JButton hamSampleButton = new JButton("Sample: ham");
    private final JButton phishingSampleButton = new JButton("Sample: phishing");
    private final JLabel statusLabel = new JLabel("Loading models, please wait...");
    private final JLabel verdictLabel = new JLabel(" ");
    private final JTextArea detailsArea = new JTextArea(9, 60);

    public EmailCheckerPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        buildPanel();
        connectActions();
        setControlsEnabled(false);
        loadModelsInBackground();
    }

    private void buildPanel() {
        emailArea.setLineWrap(true);
        emailArea.setWrapStyleWord(true);
        emailArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        verdictLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.add(new JLabel("Paste an email below (subject and body):"), BorderLayout.NORTH);
        top.add(new JScrollPane(emailArea), BorderLayout.CENTER);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.add(new JLabel("Algorithm:"));
        controls.add(algorithmBox);
        controls.add(checkButton);
        controls.add(compareButton);
        controls.add(clearButton);

        JPanel samples = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        samples.add(new JLabel("Load an example:"));
        samples.add(spamSampleButton);
        samples.add(hamSampleButton);
        samples.add(phishingSampleButton);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(controls, BorderLayout.NORTH);
        buttons.add(samples, BorderLayout.SOUTH);

        JPanel result = new JPanel(new BorderLayout(4, 4));
        result.add(statusLabel, BorderLayout.NORTH);
        result.add(verdictLabel, BorderLayout.CENTER);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setPreferredSize(new Dimension(700, 200));
        result.add(detailsScroll, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(buttons, BorderLayout.NORTH);
        bottom.add(result, BorderLayout.CENTER);

        add(top, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void connectActions() {
        checkButton.addActionListener(e -> check(false));
        compareButton.addActionListener(e -> check(true));
        clearButton.addActionListener(e -> {
            emailArea.setText("");
            detailsArea.setText("");
            verdictLabel.setText(" ");
        });
        spamSampleButton.addActionListener(e -> emailArea.setText(SAMPLE_SPAM));
        hamSampleButton.addActionListener(e -> emailArea.setText(SAMPLE_HAM));
        phishingSampleButton.addActionListener(e -> emailArea.setText(SAMPLE_PHISHING));
    }

    /** Training takes a few seconds, so it runs off the window thread (otherwise the window freezes). */
    private void loadModelsInBackground() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.initialize(DATA_PATH);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    for (String name : service.getClassifierNames()) {
                        algorithmBox.addItem(name);
                    }
                    statusLabel.setText("Ready. Paste an email and click Check.");
                    setControlsEnabled(true);
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    statusLabel.setText("Could not load " + DATA_PATH
                            + ". Run the program from the project folder.");
                    detailsArea.setText(String.valueOf(cause));
                }
            }
        }.execute();
    }

    private void check(boolean compareAll) {
        String text = emailArea.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please paste an email first.");
            return;
        }

        StringBuilder details = new StringBuilder();
        if (compareAll) {
            int spamVotes = 0;
            for (String name : service.getClassifierNames()) {
                boolean spam = service.isSpam(name, text);
                if (spam) {
                    spamVotes++;
                }
                details.append(String.format("%-28s %s%n", name + ":", spam ? "SPAM" : "NOT SPAM"));
            }
            verdictLabel.setText(spamVotes + " of 3 algorithms say SPAM");
            verdictLabel.setForeground(spamVotes >= 2 ? Color.RED : new Color(0, 130, 0));
        } else {
            String name = (String) algorithmBox.getSelectedItem();
            boolean spam = service.isSpam(name, text);
            verdictLabel.setText(spam ? "SPAM" : "NOT SPAM");
            verdictLabel.setForeground(spam ? Color.RED : new Color(0, 130, 0));
            details.append("Algorithm: ").append(name).append('\n');
        }

        details.append('\n').append(describePhishing(service.checkPhishing(text)));
        detailsArea.setText(details.toString());
        detailsArea.setCaretPosition(0);
    }

    private String describePhishing(PhishingReport report) {
        StringBuilder out = new StringBuilder("PHISHING CHECK (rule-based)\n");
        if (!report.hasIndicators()) {
            out.append("No phishing warning signs found.\n");
        } else {
            out.append(report.getIndicatorCount()).append(" warning sign(s) found:\n");
            for (String phrase : report.getPhraseMatches()) {
                out.append(" - Suspicious phrase: \"").append(phrase).append("\"\n");
            }
            for (String warning : report.getUrlWarnings()) {
                out.append(" - ").append(warning).append('\n');
            }
        }
        if (!report.getInspectedUrls().isEmpty()) {
            out.append("Links inspected: ").append(report.getInspectedUrls()).append('\n');
        }
        out.append("(Warning signs only; this is not a measured phishing accuracy.)\n");
        return out.toString();
    }

    private void setControlsEnabled(boolean enabled) {
        algorithmBox.setEnabled(enabled);
        checkButton.setEnabled(enabled);
        compareButton.setEnabled(enabled);
        spamSampleButton.setEnabled(enabled);
        hamSampleButton.setEnabled(enabled);
        phishingSampleButton.setEnabled(enabled);
    }
}