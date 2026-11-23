# EsmiringSpam
An Intelligent Spam and Phishing Detection System. A Data Structures and Algorithms
group project, written in Java with no external machine-learning libraries.

## What it does
- Reads a labeled email dataset and cleans the text.
- Classifies emails as spam or ham with three algorithms written from scratch:
  Naive Bayes, k-Nearest Neighbors (kNN) and Logistic Regression.
- Compares the three algorithms (confusion matrix, accuracy, precision, recall, F1, time).
- Checks emails for phishing warning signs using a Trie and simple link rules.
- Has a Java Swing window with two tabs: Dataset Summary (the evaluation results) and Email Checker (paste an email, choose an algorithm, click Check; also shows phishing warning signs).

## Requirements
- JDK 17 or newer (the JDK, not only the JRE). Check with: `java -version` and `javac -version`

## Compile
Open a terminal **in the project folder** (the one that contains `src` and `data`).

Windows (PowerShell):

    javac -d out (Get-ChildItem -Path src -Recurse -Filter *.java).FullName

Mac / Linux:

    javac -d out $(find src -name '*.java')

## Run
Compare the three algorithms (prints the results and saves `results.csv`):

    java -cp out Main

Open the Swing window:

    java -cp out ui.MainWindow

    **Note: ** The window takes a few seconds to load because it trains the three algorithms.

Always run from the project folder, because the program reads `data/emails.csv`.

## Project structure
    data/emails.csv                    the dataset
    src/Main.java                      runs the full comparison
    src/model/       Email.java
    src/data/        DataLoader, Deduplicator, Preprocessor, DataSplitter, PreprocessDemo
    src/classifier/  Classifier, NaiveBayes, KNN, LogisticRegression, TextVectorizer
    src/eval/        Evaluator
    src/phishing/    Trie, PhishingChecker, PhishingReport
    src/service/     SpamService
    src/ui/          MainWindow, EmailCheckerPanel

## Dataset
- Spam Email Dataset on Kaggle by jackksoncsie: https://www.kaggle.com/datasets/jackksoncsie/spam-email-dataset (accessed Nov 21, 2026)
- License: GNU Lesser General Public License v3.0
- 5,728 rows in the file; 5,695 unique emails after removing 33 duplicates
  (1,368 spam and 4,327 ham)
- Columns: `text` and `spam` (1 = spam, 0 = ham)

## Settings used
- 80% training / 20% testing, random seed 42 (the same split for all algorithms)
- kNN: k = 5, TF-IDF vectors with the 3,000 most common words
- Logistic Regression: 300 epochs, learning rate 5.0, L2 strength 0.0001

## Team
Rico Alimonsurin, Michael Juan Dy Quiangco, Rembrandt Monteclaro,
Ralph Justin Roxas, Franklin Richard Zerrudo Jr.
Instructor: Dr. Ma. Luche P. Sabayle