package DIV.bilingualsearch.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

import java.util.IllegalFormatException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BilingualSearchNames {
    private static Languages cachedLanguages;

    private record Languages(Language active, Language english, Language japanese, Language chinese,
                             String selected, Map<String, List<String>> pinyin) {}

    private BilingualSearchNames() {}

    public static Set<String> aliases(Component name, ResourceLocation id) {
        Languages languages = languages();
        Set<String> aliases = new LinkedHashSet<>();
        if (name.getContents() instanceof TranslatableContents || !name.getSiblings().isEmpty()) {
            aliases.add(ChatFormatting.stripFormatting(render(name, languages.english())).strip());
            aliases.add(ChatFormatting.stripFormatting(render(name, languages.japanese())).strip());
            addChineseAliases(aliases, name, languages);
        }
        if (id != null) {
            aliases.add(id.toString());
            aliases.add(id.getPath());
            aliases.add(id.getPath().replace('_', ' '));
        }
        aliases.remove("");
        return aliases;
    }

    public static Set<String> chineseAliases(Component name) {
        Set<String> aliases = new LinkedHashSet<>();
        addChineseAliases(aliases, name, languages());
        return aliases;
    }

    private static void addChineseAliases(Set<String> aliases, Component name, Languages languages) {
        addChineseAliases(aliases, name, languages.chinese(), languages.pinyin());
        if (languages.selected().startsWith("zh_") && languages.active() != languages.chinese()) {
            addChineseAliases(aliases, name, languages.active(), languages.pinyin());
        }
    }

    private static void addChineseAliases(Set<String> aliases, Component name, Language chinese,
                                          Map<String, List<String>> pinyin) {
        if (!hasTranslation(name, chinese)) return;
        String translated = ChatFormatting.stripFormatting(render(name, chinese)).strip();
        if (!PinyinSearch.hasHan(translated)) return;
        aliases.add(translated);
        aliases.addAll(pinyin.computeIfAbsent(translated, PinyinSearch::spellings));
    }

    private static boolean hasTranslation(Component name, Language language) {
        if (name.getContents() instanceof TranslatableContents translation && language.has(translation.getKey())) {
            return true;
        }
        for (Component sibling : name.getSiblings()) {
            if (hasTranslation(sibling, language)) return true;
        }
        return false;
    }

    private static synchronized Languages languages() {
        // LanguageManager installs a new Language on each resource reload.
        Language active = Language.getInstance();
        if (cachedLanguages == null || cachedLanguages.active() != active) {
            Minecraft client = Minecraft.getInstance();
            var resources = client.getResourceManager();
            String selected = client.getLanguageManager().getSelected();
            Language english = selected.equals("en_us") ? active
                    : ClientLanguage.loadFrom(resources, List.of("en_us"), false);
            Language japanese = selected.equals("ja_jp") ? active
                    : ClientLanguage.loadFrom(resources, List.of("en_us", "ja_jp"), false);
            Language chinese = selected.equals("zh_cn") ? active
                    : ClientLanguage.loadFrom(resources, List.of("zh_cn"), false);
            cachedLanguages = new Languages(active, english, japanese, chinese, selected, new ConcurrentHashMap<>());
        }
        return cachedLanguages;
    }

    private static String render(Component component, Language language) {
        StringBuilder text = new StringBuilder();
        if (component.getContents() instanceof TranslatableContents translation) {
            String fallback = translation.getFallback();
            String template = language.getOrDefault(translation.getKey(),
                    fallback == null ? translation.getKey() : fallback);
            Object[] args = translation.getArgs().clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component nested) args[i] = render(nested, language);
            }
            try {
                text.append(String.format(Locale.ROOT, template, args));
            } catch (IllegalFormatException ignored) {
                text.append(template);
            }
        } else {
            component.getContents().visit(part -> {
                text.append(part);
                return Optional.empty();
            });
        }
        for (Component sibling : component.getSiblings()) text.append(render(sibling, language));
        return text.toString();
    }
}
