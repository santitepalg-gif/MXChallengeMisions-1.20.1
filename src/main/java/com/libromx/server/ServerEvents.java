package com.libromx.server;

import com.libromx.LibroMX;
import com.libromx.data.BookData;
import com.libromx.net.Net;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = LibroMX.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onStarting(ServerStartingEvent e) {
        BookData.load(e.getServer());
        Progress.load(e.getServer());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        BookData.save(e.getServer());
        Progress.save(e.getServer());
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        BookCommands.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            Net.sync(sp, false);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof ServerPlayer sp) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().getType());
            if (id != null) Progress.onKill(sp, id);
        }
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            Progress.onCraft(sp, e.getCrafting());
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer sp && sp.tickCount % 20 == 0) {
            Progress.checkCollect(sp);
        }
    }
}
