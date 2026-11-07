package com.libromx.client;

import com.libromx.data.BookData;
import com.libromx.data.PlayerProgress;
import com.libromx.net.SyncPacket;
import net.minecraft.client.Minecraft;

public class ClientHooks {
    public static void onSync(SyncPacket m) {
        ClientData.data = BookData.fromJson(m.bookJson);
        ClientData.progress = PlayerProgress.fromJson(m.progressJson);
        ClientData.admin = m.admin;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BookScreen bs) {
            bs.onData();
        } else if (m.open && mc.screen == null) {
            mc.setScreen(new BookScreen());
        }
    }
}
