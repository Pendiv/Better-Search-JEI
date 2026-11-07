package DIV.bilingualsearch.mixin;

import DIV.bilingualsearch.LegacySearch;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class BilingualSearchMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return switch (mixinClassName) {
            case "DIV.bilingualsearch.mixin.CreativeSearchNamesMixin" -> !LegacySearch.provides(LegacySearch.CREATIVE_ALIASES);
            case "DIV.bilingualsearch.mixin.JeiImeSearchMixin" -> !LegacySearch.provides(LegacySearch.JEI_IME);
            case "DIV.bilingualsearch.mixin.LegacyImeSearchTermsMixin" ->
                    LegacySearch.provides(LegacySearch.JEI_IME) && LegacySearch.provides(LegacySearch.JEI_IME_TERMS);
            default -> true;
        };
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
