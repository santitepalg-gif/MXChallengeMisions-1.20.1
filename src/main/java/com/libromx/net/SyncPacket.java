package com.libromx.net;

import com.libromx.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPacket {
    private static final int MAX = 262144;

    public final String bookJson;
    public final String progressJson;
    public final boolean admin;
    public final boolean open;

    public SyncPacket(String bookJson, String progressJson, boolean admin, boolean open) {
        this.bookJson = bookJson;
        this.progressJson = progressJson;
        this.admin = admin;
        this.open = open;
    }

    public static void encode(SyncPacket m, FriendlyByteBuf b) {
        b.writeUtf(m.bookJson, MAX);
        b.writeUtf(m.progressJson, MAX);
        b.writeBoolean(m.admin);
        b.writeBoolean(m.open);
    }

    public static SyncPacket decode(FriendlyByteBuf b) {
        return new SyncPacket(b.readUtf(MAX), b.readUtf(MAX), b.readBoolean(), b.readBoolean());
    }

    public static void handle(SyncPacket m, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx = sup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.onSync(m)));
        ctx.setPacketHandled(true);
    }
}
