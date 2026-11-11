package nl.saxion.languagetool;

import nl.saxion.app.SaxionApp;
import nl.saxion.languagetool.analysis.SentenceAnalyzer;
import nl.saxion.languagetool.io.Printer;
import nl.saxion.languagetool.model.Token;
import nl.saxion.languagetool.rules.RuleEngine;
import nl.saxion.languagetool.rules.RuleType;
import nl.saxion.languagetool.translation.Dictionary;
import nl.saxion.languagetool.translation.Translator;

import java.util.List;

public class Application implements Runnable {

    public static void main(String[] args) {
        SaxionApp.start(new Application(), 800, 800);
    }

    public void run() {

        SaxionApp.printLine("=== Babel Gavel – Language Structure Explorer ===");
        SaxionApp.printLine("Enter an English sentence:");
        String input = SaxionApp.readString();

        /* =========================
           Step 1 – Analyse sentence
           ========================= */

        SentenceAnalyzer analyzer = new SentenceAnalyzer();
        List<Token> analysedTokens = analyzer.analyze(input);

        Printer.printAnalysis(analysedTokens);

        SaxionApp.pause();

        /* =========================
           Step 2 – Literal translation
           ========================= */

        Dictionary dictionary = new Dictionary("/data/en_nl_dictionary.csv");
        Translator translator = new Translator(dictionary);

        List<Token> translatedTokens = translator.translate(analysedTokens);

        Printer.printTranslatedSentence(translatedTokens);

        /* =========================
           Step 3 – Choose and apply rule
           ========================= */

        SaxionApp.printLine("");
        SaxionApp.printLine("Choose a rule to apply:");
        SaxionApp.printLine("1 - Verb Second (V2)");
        SaxionApp.printLine("2 - Negation");
        SaxionApp.printLine("3 - Adverbial First");
        SaxionApp.printLine("4 - Auxiliary Verb Order");
        SaxionApp.printLine("5 - Separable Verb");

        int choice = SaxionApp.readInt();

        RuleType selectedRule = RuleType.fromChoice(choice);

        RuleEngine engine = new RuleEngine();
        List<Token> afterRule = engine.applyRule(selectedRule, translatedTokens);

        Printer.printAfterRule(selectedRule.getDisplayName(), afterRule);

        /* =========================
           Limitations notice
           ========================= */

        Printer.printNote(
                "This tool applies one rule at a time.\n" +
                        "Real language uses multiple rules at once."
        );

        // SaxionApp.stop();  // no longer needed
    }
}
