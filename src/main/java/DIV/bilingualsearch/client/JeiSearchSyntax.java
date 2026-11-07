package DIV.bilingualsearch.client;

public final class JeiSearchSyntax {
    private JeiSearchSyntax() {}

    /** Normalize IME punctuation only where JEI reads search syntax. */
    public static String normalize(String query) {
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
