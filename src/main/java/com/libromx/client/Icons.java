package com.libromx.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

public final class Icons {
    private Icons() {
    }

    /** Icono para un objetivo: huevo de spawn si es un mob, o el item. */
    public static ItemStack stack(boolean entity, ResourceLocation id) {
        if (entity) {
            if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) return new ItemStack(Items.SKELETON_SKULL);
            EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(id);
            SpawnEggItem egg = t == null ? null : SpawnEggItem.byId(t);
            return egg != null ? new ItemStack(egg) : new ItemStack(Items.SKELETON_SKULL);
        }
        Item it = ForgeRegistries.ITEMS.getValue(id);
        return (it == null || it == Items.AIR) ? new ItemStack(Items.BARRIER) : new ItemStack(it);
    }
}
