package com.libromx.net;

import com.libromx.data.BookData;
import com.libromx.data.Entry;
import com.libromx.server.Actions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EditPacket {
    public static final int ADD = 0;
    public static final int REMOVE = 1;
    public static final int APPLY = 2;
    public static final int REPLACE = 3;

    public final int action;
    public final int book;
    public final int index;
    public final String entry;

    public EditPacket(int action, int book, int index, String entry) {
        this.action = action;
        this.book = book;
        this.index = index;
        this.entry = entry == null ? "" : entry;
    }

    public static EditPacket of(int action, int book, int index, Entry e) {
        return new EditPacket(action, book, index, e == null ? "" : BookData.NET_GSON.toJson(e));
    }

    public static void encode(EditPacket m, FriendlyByteBuf b) {
        b.writeByte(m.action);
        b.writeByte(m.book);
        b.writeInt(m.index);
        b.writeUtf(m.entry, 32767);
    }

    public static EditPacket decode(FriendlyByteBuf b) {
        return new EditPacket(b.readByte(), b.readByte(), b.readInt(), b.readUtf(32767));
    }

    public static void handle(EditPacket m, Supplier<NetworkEvent.Context> sup) {
        NetworkEvent.Context ctx = sup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer p = ctx.getSender();
            if (p == null || !p.hasPermissions(2)) return;
            MinecraftServer s = p.getServer();
            if (s == null) return;
            Entry e = null;
            if (!m.entry.isEmpty()) {
                try {
                    e = BookData.NET_GSON.fromJson(m.entry, Entry.class);
                } catch (Exception ex) {
                    return;
                }
            }
            switch (m.action) {
                case ADD -> {
                    if (e != null) Actions.add(s, m.book, e);
                }
                case REPLACE -> {
                    if (e != null) Actions.replace(s, m.book, m.index, e);
                }
                case REMOVE -> Actions.remove(s, m.book, m.index);
                case APPLY -> Actions.applyDifficulty(s, m.index);
                default -> {
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
