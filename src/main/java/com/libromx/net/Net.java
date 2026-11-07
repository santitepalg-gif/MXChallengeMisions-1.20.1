package com.libromx.net;

import com.libromx.LibroMX;
import com.libromx.data.BookData;
import com.libromx.server.Progress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Net {
    private static final String PROTOCOL = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LibroMX.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, RequestPacket.class, RequestPacket::encode, RequestPacket::decode, RequestPacket::handle);
        CHANNEL.registerMessage(id++, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
        CHANNEL.registerMessage(id++, EditPacket.class, EditPacket::encode, EditPacket::decode, EditPacket::handle);
    }

    public static void sync(ServerPlayer p, boolean open) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new SyncPacket(BookData.server().toJson(), Progress.json(p), p.hasPermissions(2), open));
    }

    public static void syncAll(MinecraftServer s) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            sync(p, false);
        }
    }
}
