package DIV.bilingualsearch.jei;

import DIV.bilingualsearch.BilingualSearch;
import DIV.bilingualsearch.LegacySearch;
import DIV.bilingualsearch.client.BilingualSearchNames;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.registration.IIngredientAliasRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public final class BilingualSearchJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(BilingualSearch.MOD_ID, "aliases");
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        if (LegacySearch.provides(LegacySearch.JEI_ALIASES)) return;

        var itemStacks = ItemStackLinkedSet.createTypeAndTagSet();
        for (Item item : ForgeRegistries.ITEMS.getValues()) itemStacks.add(item.getDefaultInstance());
        for (var tab : CreativeModeTabs.allTabs()) {
            itemStacks.addAll(tab.getDisplayItems());
            itemStacks.addAll(tab.getSearchTabDisplayItems());
        }
        for (ItemStack stack : itemStacks) {
            if (stack.isEmpty()) continue;
            var aliases = BilingualSearchNames.aliases(stack.getHoverName(), ForgeRegistries.ITEMS.getKey(stack.getItem()));
            if (!aliases.isEmpty()) registration.addAliases(VanillaTypes.ITEM_STACK, stack, aliases);
        }
        for (var fluid : ForgeRegistries.FLUIDS.getValues()) {
            if (!fluid.defaultFluidState().isSource()) continue;
            FluidStack stack = new FluidStack(fluid, 1000);
            if (stack.isEmpty()) continue;
            var aliases = BilingualSearchNames.aliases(stack.getDisplayName(), ForgeRegistries.FLUIDS.getKey(fluid));
            if (!aliases.isEmpty()) registration.addAliases(ForgeTypes.FLUID_STACK, stack, aliases);
        }
    }
}
