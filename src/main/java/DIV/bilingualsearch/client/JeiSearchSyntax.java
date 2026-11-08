package DIV.bilingualsearch.client;

import java.util.List;

public final class JeiSearchSyntax {
    private enum Dialect { LEGACY, TOKENIZER, QUOTE_AWARE_OPERATORS }

    private static final Dialect DIALECT = detectDialect();

    private JeiSearchSyntax() {}

    /** Normalize IME punctuation only where JEI reads search syntax. */
    public static String normalize(String query) {
        return DIALECT == Dialect.LEGACY ? normalizeLegacy(query) : normalizeTokenizer(query);
    }

    private static Dialect detectDialect() {
        Class<?> tokenizer;
        try {
            tokenizer = Class.forName("mezz.jei.gui.search.SearchTokenizer", false, JeiSearchSyntax.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return Dialect.LEGACY;
        }
        try {
            tokenizer.getMethod("splitByOperators", List.class);
            return Dialect.QUOTE_AWARE_OPERATORS;
        } catch (NoSuchMethodException e) {
            return Dialect.TOKENIZER;
        }
    }

    private static String normalizeLegacy(String query) {
        StringBuilder normalized = null;
        boolean atTermStart = true;
        boolean afterMinus = false;
        char closingQuote = '\0';

        for (int i = 0; i < query.length(); i++) {
            char original = query.charAt(i);
            char character = original;
            if (original == '|' || (original == '｜' && closingQuote == '\0')) {
                // JEI splits ASCII bars before tokenizing the phrases.
                character = '|';
                closingQuote = '\0';
                atTermStart = true;
                afterMinus = false;
            } else if (closingQuote != '\0') {
                if (original == closingQuote || original == '"') {
                    character = '"';
                    closingQuote = '\0';
                    atTermStart = true;
                    afterMinus = false;
                } else if (atTermStart) {
                    character = normalizePrefix(original);
                    atTermStart = false;
                }
            } else if (isSeparator(original)) {
                if (original == '　') character = ' ';
                atTermStart = true;
                afterMinus = false;
            } else if (atTermStart) {
                char quote = matchingQuote(original);
                if (quote != '\0') {
                    character = '"';
                    closingQuote = quote;
                } else if (!afterMinus && (original == '-' || original == '－' || original == '−')) {
                    character = '-';
                    afterMinus = true;
                } else {
                    character = normalizePrefix(original);
                    atTermStart = false;
                }
            }

            if (normalized == null && original != character) {
                normalized = new StringBuilder(query.length()).append(query, 0, i);
            }
            if (normalized != null) normalized.append(character);
        }

        return normalized == null ? query : normalized.toString();
    }

    private static String normalizeTokenizer(String query) {
        StringBuilder normalized = null;
        boolean splitBarsFirst = DIALECT == Dialect.TOKENIZER;
        boolean escaped = false;
        char closingQuote = '\0';
        int termLength = 0;
        boolean onlyPrefix = false;

        for (int i = 0; i < query.length(); i++) {
            char original = query.charAt(i);
            char character = original;
            if (splitBarsFirst && original == '|') {
                // The older tokenizer only receives one pre-split branch.
                closingQuote = '\0';
                escaped = false;
                termLength = 0;
                onlyPrefix = false;
            } else if (escaped) {
                escaped = false;
                termLength++;
                onlyPrefix = false;
            } else if (original == '\\') {
                escaped = true;
            } else if (closingQuote != '\0') {
                if (original == closingQuote || original == '"') {
                    character = '"';
                    closingQuote = '\0';
                    termLength = 0;
                    onlyPrefix = false;
                } else if (termLength == 0 && (!splitBarsFirst || !Character.isWhitespace(original))) {
                    character = normalizePrefix(original);
                    termLength++;
                }
            } else if (original == '|' || original == '｜' || Character.isWhitespace(original)) {
                if (original == '｜') character = '|';
                if (original == '　') character = ' ';
                termLength = 0;
                onlyPrefix = false;
            } else if (original == '"' || ((termLength == 0 || onlyPrefix) && matchingQuote(original) != '\0')) {
                character = '"';
                if (splitBarsFirst || termLength == 0 || onlyPrefix) closingQuote = matchingQuote(original);
                else termLength = 0;
            } else if (termLength == 0 && (original == '-' || original == '－' || original == '−')) {
                character = '-';
            } else {
                if (termLength == 0) character = normalizePrefix(original);
                termLength++;
                onlyPrefix = termLength == 1 && "@#$%^&".indexOf(character) >= 0;
            }

            if (normalized == null && original != character) {
                normalized = new StringBuilder(query.length()).append(query, 0, i);
            }
            if (normalized != null) normalized.append(character);
        }
        return normalized == null ? query : normalized.toString();
    }

    private static char normalizePrefix(char character) {
        return switch (character) {
            case '＠' -> '@';
            case '＃' -> '#';
            case '＄' -> '$';
            case '％' -> '%';
            case '＾' -> '^';
            case '＆' -> '&';
            default -> character;
        };
    }

    private static char matchingQuote(char character) {
        return switch (character) {
            case '"', '＂' -> character;
            case '“', '”' -> '”';
            default -> '\0';
        };
    }

    private static boolean isSeparator(char character) {
        return character == ' ' || character == '　' || (character >= '\t' && character <= '\r');
    }
}
