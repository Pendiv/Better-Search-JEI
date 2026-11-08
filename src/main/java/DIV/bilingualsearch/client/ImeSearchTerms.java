package DIV.bilingualsearch.client;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

public final class ImeSearchTerms {
    private static final int MAX_ROMANIZATIONS = 16;
    private static final int MAX_VARIANT_ATTEMPTS = MAX_ROMANIZATIONS * 4;
    // An empty form repeats the preceding vowel when there is one.
    private static final List<String> LONG_VOWEL_FORMS = List.of("-", "");
    private static final Map<String, List<String>> READINGS = readings();

    private ImeSearchTerms() {}

    /** Fold the fullwidth counterparts of printable ASCII and space. */
    private static String foldWidth(String text) {
        StringBuilder folded = null;
        for (int i = 0; i < text.length(); i++) {
            char original = text.charAt(i);
            char character = original;
            if (original >= '\uff01' && original <= '\uff5e') character = (char) (original - 0xfee0);
            if (original == '\u3000') character = ' ';
            if (folded == null && original != character) {
                folded = new StringBuilder(text.length()).append(text, 0, i);
            }
            if (folded != null) folded.append(character);
        }
        return folded == null ? text : folded.toString();
    }

    /** Alternatives to a single parsed search term. The caller retains the original term. */
    public static List<String> alternatives(String term) {
        String asciiFolded = foldWidth(term);
        String widthFolded = foldKanaWidth(asciiFolded);
        boolean romanize = hasKanaWithoutKanji(widthFolded);
        if (!romanize && widthFolded.equals(asciiFolded)) {
            return asciiFolded.equals(term) ? List.of() : List.of(asciiFolded);
        }

        var result = new LinkedHashSet<String>();
        result.add(asciiFolded);
        result.add(widthFolded);
        if (romanize) {
            String kana = normalizeKana(widthFolded);
            result.add(kana);
            addRomanizations(kana, result);
        }
        result.remove(term);
        return List.copyOf(result);
    }

    private record ReadingChange(int position, int option) {
        int at(int index) {
            return index == position ? option : 0;
        }
    }

    private static void addRomanizations(String kana, Set<String> result) {
        List<List<String>> segments = readSegments(kana);
        int maxOptions = 0;
        int combinations = 1;
        for (var segment : segments) {
            maxOptions = Math.max(maxOptions, segment.size());
            combinations = Math.min(MAX_ROMANIZATIONS + 1, combinations * segment.size());
        }
        if (combinations <= MAX_ROMANIZATIONS) {
            addSmallReadingSet(segments, combinations, result);
            return;
        }

        var variants = new LinkedHashSet<String>();
        variants.add(render(segments, index -> 0));
        var changes = new ArrayList<ReadingChange>();
        int attempts = 1;
        for (int option = 1; option < maxOptions && attempts < MAX_VARIANT_ATTEMPTS
                && variants.size() < MAX_ROMANIZATIONS; option++) {
            // Give both ends of the term a turn before trying a kana's next spelling.
            for (int offset = 0; offset < segments.size(); offset++) {
                int position = offset % 2 == 0 ? offset / 2 : segments.size() - 1 - offset / 2;
                if (segments.get(position).size() <= option) continue;
                var change = new ReadingChange(position, option);
                changes.add(change);
                variants.add(render(segments, change::at));
                if (++attempts == MAX_VARIANT_ATTEMPTS || variants.size() == MAX_ROMANIZATIONS) break;
            }
        }

        // A consistent alternate spelling can differ at more than two positions.
        if (changes.size() > 2 && attempts < MAX_VARIANT_ATTEMPTS && variants.size() < MAX_ROMANIZATIONS) {
            variants.add(render(segments, index -> segments.get(index).size() > 1 ? 1 : 0));
            attempts++;
        }
        for (int i = 0; i < changes.size() && attempts < MAX_VARIANT_ATTEMPTS
                && variants.size() < MAX_ROMANIZATIONS; i++) {
            var first = changes.get(i);
            for (int j = i + 1; j < changes.size(); j++) {
                var second = changes.get(j);
                if (first.position() == second.position()) continue;
                variants.add(render(segments, index -> first.at(index) + second.at(index)));
                if (++attempts == MAX_VARIANT_ATTEMPTS || variants.size() == MAX_ROMANIZATIONS) break;
            }
        }
        result.addAll(variants);
    }

    private static void addSmallReadingSet(List<List<String>> segments, int combinations, Set<String> result) {
        int[] choices = new int[segments.size()];
        for (int variant = 0; variant < combinations; variant++) {
            result.add(render(segments, index -> choices[index]));
            for (int index = choices.length - 1; index >= 0; index--) {
                if (++choices[index] < segments.get(index).size()) break;
                choices[index] = 0;
            }
        }
    }

    private static List<List<String>> readSegments(String kana) {
        var segments = new ArrayList<List<String>>();
        for (int i = 0; i < kana.length();) {
            char character = kana.charAt(i);
            if (character == 'ー') {
                segments.add(LONG_VOWEL_FORMS);
                i++;
                continue;
            }

            boolean doubled = character == 'っ';
            int start = doubled ? i + 1 : i;
            int end = Math.min(kana.length(), start + 2);
            List<String> options = end - start == 2 ? READINGS.get(kana.substring(start, end)) : null;
            if (options == null && start < kana.length()) {
                end = start + 1;
                options = READINGS.get(kana.substring(start, end));
            }

            if (doubled) {
                var combinations = doubleConsonant(options);
                if (combinations.isEmpty()) {
                    options = READINGS.get("っ");
                    end = i + 1;
                } else {
                    options = combinations;
                }
            }
            if (options == null) {
                segments.add(List.of(String.valueOf(character)));
                i++;
            } else {
                segments.add(options);
                i = end;
            }
        }
        return segments;
    }

    private static String render(List<List<String>> segments, IntUnaryOperator optionAt) {
        var result = new StringBuilder(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            String form = segments.get(i).get(optionAt.applyAsInt(i));
            if (!form.isEmpty()) {
                result.append(form);
            } else {
                char previous = result.isEmpty() ? '-' : result.charAt(result.length() - 1);
                result.append("aeiou".indexOf(previous) >= 0 ? previous : '-');
            }
        }
        return result.toString();
    }

    private static boolean hasKanaWithoutKanji(String term) {
        boolean hasKana = false;
        for (int i = 0; i < term.length();) {
            int cp = term.codePointAt(i);
            if (cp > 127 && Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) return false;
            if ((cp >= 0x3041 && cp <= 0x30fa) || cp == 'ー' || (cp >= 0xff66 && cp <= 0xff9f)) hasKana = true;
            i += Character.charCount(cp);
        }
        return hasKana;
    }

    private static String foldKanaWidth(String text) {
        StringBuilder result = null;
        for (int i = 0; i < text.length();) {
            char character = text.charAt(i);
            if (character >= '\uff66' && character <= '\uff9f') {
                if (result == null) result = new StringBuilder(text.length()).append(text, 0, i);
                int end = i + 1;
                while (end < text.length() && text.charAt(end) >= '\uff66' && text.charAt(end) <= '\uff9f') end++;
                result.append(Normalizer.normalize(text.substring(i, end), Normalizer.Form.NFKC));
                i = end;
            } else {
                if (result != null) result.append(character);
                i++;
            }
        }
        return result == null ? text : result.toString();
    }

    private static String normalizeKana(String text) {
        String composed = Normalizer.normalize(text, Normalizer.Form.NFC);
        var result = new StringBuilder(composed.length());
        for (int i = 0; i < composed.length(); i++) {
            char character = composed.charAt(i);
            if (character >= 'ァ' && character <= 'ヶ') character -= 0x60;
            result.append(character);
        }
        return result.toString();
    }

    private static List<String> doubleConsonant(List<String> options) {
        if (options == null) return List.of();
        var result = new ArrayList<String>();
        for (String reading : options) {
            char first = reading.charAt(0);
            if ("aeiounxl".indexOf(first) < 0) result.add(first + reading);
        }
        if (!result.isEmpty() && options.get(0).startsWith("ch")) result.add("t" + options.get(0));
        return result;
    }

    private static Map<String, List<String>> readings() {
        String table = """
                あ:a い:i う:u,wu え:e お:o
                か:ka,ca き:ki く:ku,cu,qu け:ke こ:ko,co
                さ:sa し:shi,si,ci す:su せ:se,ce そ:so
                た:ta ち:chi,ti つ:tsu,tu て:te と:to
                な:na に:ni ぬ:nu ね:ne の:no
                は:ha ひ:hi ふ:fu,hu へ:he ほ:ho
                ま:ma み:mi む:mu め:me も:mo
                や:ya ゆ:yu よ:yo
                ら:ra り:ri る:ru れ:re ろ:ro
                わ:wa ゐ:wi ゑ:we を:wo,o ん:n,nn,n',xn
                が:ga ぎ:gi ぐ:gu げ:ge ご:go
                ざ:za じ:ji,zi ず:zu ぜ:ze ぞ:zo
                だ:da ぢ:di,ji づ:du,zu で:de ど:do
                ば:ba び:bi ぶ:bu べ:be ぼ:bo
                ぱ:pa ぴ:pi ぷ:pu ぺ:pe ぽ:po ゔ:vu
                ぁ:xa,la ぃ:xi,li ぅ:xu,lu ぇ:xe,le ぉ:xo,lo
                ゃ:xya,lya ゅ:xyu,lyu ょ:xyo,lyo っ:xtu,ltu,xtsu,ltsu
                ゎ:xwa,lwa ゕ:xka ゖ:xke
                きゃ:kya きゅ:kyu きょ:kyo きぇ:kye
                ぎゃ:gya ぎゅ:gyu ぎょ:gyo ぎぇ:gye
                しゃ:sha,sya しゅ:shu,syu しょ:sho,syo しぇ:she,sye
                じゃ:ja,jya,zya じゅ:ju,jyu,zyu じょ:jo,jyo,zyo じぇ:je,jye,zye
                ちゃ:cha,tya,cya ちゅ:chu,tyu,cyu ちょ:cho,tyo,cyo ちぇ:che,tye,cye
                にゃ:nya にゅ:nyu にょ:nyo にぇ:nye
                ひゃ:hya ひゅ:hyu ひょ:hyo ひぇ:hye
                びゃ:bya びゅ:byu びょ:byo びぇ:bye
                ぴゃ:pya ぴゅ:pyu ぴょ:pyo ぴぇ:pye
                みゃ:mya みゅ:myu みょ:myo みぇ:mye
                りゃ:rya りゅ:ryu りょ:ryo りぇ:rye
                ぢゃ:dya ぢゅ:dyu ぢょ:dyo
                ふぁ:fa ふぃ:fi ふぇ:fe ふぉ:fo ふゅ:fyu
                ゔぁ:va ゔぃ:vi ゔぇ:ve ゔぉ:vo ゔゅ:vyu
                うぃ:wi,whi うぇ:we,whe うぉ:wo,who いぇ:ye
                くぁ:qa,kwa くぃ:qi,kwi くぇ:qe,kwe くぉ:qo,kwo
                ぐぁ:gwa ぐぃ:gwi ぐぇ:gwe ぐぉ:gwo
                すぃ:swi ずぃ:zwi
                てぃ:ti,thi てゅ:thu でぃ:di,dhi でゅ:dhu
                とぅ:tu,twu どぅ:du,dwu
                つぁ:tsa つぃ:tsi つぇ:tse つぉ:tso
                """;
        var result = new HashMap<String, List<String>>();
        for (String entry : table.strip().split("\\s+")) {
            int separator = entry.indexOf(':');
            result.put(entry.substring(0, separator), List.of(entry.substring(separator + 1).split(",")));
        }
        return Map.copyOf(result);
    }
}
