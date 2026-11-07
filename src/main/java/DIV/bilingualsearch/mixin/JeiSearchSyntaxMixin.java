package DIV.bilingualsearch.mixin;

import DIV.bilingualsearch.client.JeiSearchSyntax;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "mezz.jei.gui.ingredients.IngredientFilter", remap = false)
public abstract class JeiSearchSyntaxMixin {
    @ModifyVariable(method = "getIngredientListUncached(Ljava/lang/String;)Ljava/util/stream/Stream;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String bilingualsearch$normalizeSearchSyntax(String query) {
        return JeiSearchSyntax.normalize(query);
    }
}
