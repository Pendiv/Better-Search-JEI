package DIV.bilingualsearch.mixin;

import DIV.bilingualsearch.client.BilingualSearchNames;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.Set;

@Pseudo
@Mixin(targets = "DIV.gtcsolo.client.BilingualSearchNames", remap = false)
public abstract class LegacyChineseSearchNamesMixin {
    @Inject(method = "aliases(Lnet/minecraft/network/chat/Component;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Set;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static void bilingualsearch$includeChineseAliases(Component name, ResourceLocation id,
            CallbackInfoReturnable<Set<String>> callback) {
        var chinese = BilingualSearchNames.chineseAliases(name);
        if (chinese.isEmpty()) return;
        var aliases = new LinkedHashSet<>(callback.getReturnValue());
        aliases.addAll(chinese);
        callback.setReturnValue(aliases);
    }
}
