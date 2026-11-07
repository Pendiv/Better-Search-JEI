package DIV.bilingualsearch;

import net.minecraftforge.fml.loading.LoadingModList;

import java.nio.file.Files;

/** Select a single provider for each feature when installed alongside a GTCsoloEU build. */
public final class LegacySearch {
    public static final String JEI_ALIASES = "DIV/gtcsolo/client/BilingualSearchNames.class";
    public static final String CREATIVE_ALIASES = "DIV/gtcsolo/mixin/CreativeSearchNamesMixin.class";
    public static final String JEI_IME = "DIV/gtcsolo/mixin/jei/JeiImeSearchMixin.class";
    public static final String JEI_IME_TERMS = "DIV/gtcsolo/client/ImeSearchTerms.class";

    private LegacySearch() {}

    public static boolean provides(String resource) {
        var loadingMods = LoadingModList.get();
        if (loadingMods == null) return LegacySearch.class.getClassLoader().getResource(resource) != null;

        var gtcsolo = loadingMods.getModFileById("gtcsolo");
        return gtcsolo != null && Files.isRegularFile(gtcsolo.getFile().findResource(resource));
    }
}
