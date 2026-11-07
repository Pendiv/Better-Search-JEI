package DIV.bilingualsearch.mixin;

import DIV.bilingualsearch.client.BilingualSearchNames;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(FullTextSearchTree.class)
public abstract class CreativeSearchNamesMixin<T> {
    @ModifyArg(method = "refresh", index = 1, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/searchtree/PlainTextSearchTree;create(Ljava/util/List;Ljava/util/function/Function;)Lnet/minecraft/client/searchtree/PlainTextSearchTree;"))
    private Function<T, Stream<String>> bilingualsearch$includeSearchNames(Function<T, Stream<String>> names) {
        return entry -> {
            Stream<String> original = names.apply(entry);
            if (!(entry instanceof ItemStack stack) || stack.isEmpty()) return original;
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return Stream.concat(original, BilingualSearchNames.aliases(stack.getHoverName(), id).stream()).distinct();
        };
    }
}
