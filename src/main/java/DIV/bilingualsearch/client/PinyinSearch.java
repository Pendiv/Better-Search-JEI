package DIV.bilingualsearch.client;

import com.ibm.icu.text.Transliterator;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class PinyinSearch {
    private static final Transliterator HAN_LATIN = Transliterator.getInstance("Han-Latin");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern WORD_SEPARATORS = Pattern.compile("[^a-z0-9]+");

    private PinyinSearch() {}

    public static List<String> spellings(String chinese) {
        if (!hasHan(chinese)) return List.of();
        String latin = HAN_LATIN.transliterate(chinese);
        if (hasHan(latin)) return List.of();
        String decomposed = Normalizer.normalize(latin, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);

        Set<String> spellings = new LinkedHashSet<>();
        addForms(spellings, decomposed.replace("u\u0308", "v"));
        addForms(spellings, decomposed);
        return List.copyOf(spellings);
    }

    static boolean hasHan(String name) {
        return name.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private static void addForms(Set<String> spellings, String decomposed) {
        String plain = MARKS.matcher(decomposed).replaceAll("");
        String spaced = WORD_SEPARATORS.matcher(plain).replaceAll(" ").strip();
        if (spaced.isEmpty()) return;
        spellings.add(spaced);
        spellings.add(spaced.replace(" ", ""));
    }
}
