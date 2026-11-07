package com.libromx.client;

import com.libromx.LibroMX;
import com.libromx.net.Net;
import com.libromx.net.RequestPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LibroMX.MODID, value = Dist.CLIENT)
public class ClientForgeEvents {
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (ClientModEvents.OPEN.consumeClick()) {
            if (mc.screen == null && mc.player != null) {
                Net.CHANNEL.sendToServer(new RequestPacket());
            }
        }
    }
}
