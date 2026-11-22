# EsmiringSpam
An Intelligent Spam and Phishing Detection System (DSA Project)

## Requirements
- Java 17 or newer (JDK, not just JRE)

## Compile
From the project root:

    # Windows (PowerShell)
    dir src -Recurse -Filter *.java | % { $_.FullName } > srcs.txt
    javac -d out "@srcs.txt"

    # Mac / Linux
    find src -name '*.java' > srcs.txt
    javac -d out @srcs.txt

## Run (compare the 3 algorithms)
    java -cp out Main

This prints each algorithm's confusion matrix and metrics and writes `results.csv`.

## Run the GUI
    java -cp out ui.MainWindow

## Project structure
(paste the folder tree here and one line about each package)

## Dataset
(source, link, license, number of emails, spam/ham counts. See Phase 2)

## Team
(names and roles)