package DIV.bilingualsearch.mixin;

import DIV.bilingualsearch.client.ImeSearchTerms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Use the current readings even when GTCsoloEU supplies the JEI hook. */
@Pseudo
@Mixin(targets = "DIV.gtcsolo.client.ImeSearchTerms", remap = false)
public abstract class LegacyImeSearchTermsMixin {
    @Inject(method = "alternatives(Ljava/lang/String;)Ljava/util/List;", at = @At("HEAD"),
            cancellable = true, require = 0)
    private static void bilingualsearch$useCurrentReadings(String term, CallbackInfoReturnable<List<String>> callback) {
        callback.setReturnValue(ImeSearchTerms.alternatives(term));
    }
}
