package com.libromx.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestPacket {
    public RequestPacket() {
    }

    public static void encode(RequestPacket m, FriendlyByteBuf b) {
    }

    public static RequestPacket decode(FriendlyByteBuf b) {
        return new RequestPacket();
    }

    public static void handle(RequestPacket m, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx = sup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer p = ctx.getSender();
            if (p != null) Net.sync(p, true);
        });
        ctx.setPacketHandled(true);
    }
}
