package com.libromx.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.libromx.LibroMX;
import com.libromx.data.BookData;
import com.libromx.data.Entry;
import com.libromx.data.PlayerProgress;
import com.libromx.net.Net;
import com.libromx.util.TextFmt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Progreso de misiones por jugador (servidor). Al completar una mision se entregan las recompensas. */
public class Progress {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, PlayerProgress>>() {
    }.getType();
    private static Map<String, PlayerProgress> all = new HashMap<>();

    private static Path path(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("libromx_progress.json");
    }

    public static void load(MinecraftServer s) {
        all = new HashMap<>();
        try {
            Path p = path(s);
            if (Files.exists(p)) {
                Map<String, PlayerProgress> m = GSON.fromJson(Files.readString(p), MAP_TYPE);
                if (m != null) {
                    all = m;
                    for (PlayerProgress pp : all.values()) pp.fix();
                }
            }
        } catch (Exception e) {
            LibroMX.LOGGER.error("No se pudo leer libromx_progress.json", e);
        }
    }

    public static void save(MinecraftServer s) {
        try {
            Files.writeString(path(s), GSON.toJson(all, MAP_TYPE));
        } catch (Exception e) {
            LibroMX.LOGGER.error("No se pudo guardar libromx_progress.json", e);
        }
    }

    public static PlayerProgress of(ServerPlayer p) {
        return all.computeIfAbsent(p.getUUID().toString(), k -> new PlayerProgress());
    }

    public static String json(ServerPlayer p) {
        return BookData.NET_GSON.toJson(of(p));
    }

    public static void reset(ServerPlayer p) {
        all.remove(p.getUUID().toString());
        Net.sync(p, false);
    }

    public static void resetAll(MinecraftServer s) {
        all.clear();
        save(s);
        Net.syncAll(s);
    }

    public static void onKill(ServerPlayer p, ResourceLocation entityId) {
        if (advance(p, "kill", entityId.toString(), 1)) Net.sync(p, false);
    }

    public static void onCraft(ServerPlayer p, ItemStack crafted) {
        if (crafted.isEmpty()) return;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(crafted.getItem());
        if (id != null && advance(p, "craft", id.toString(), crafted.getCount())) Net.sync(p, false);
    }

    private static boolean advance(ServerPlayer p, String type, String id, int amount) {
        PlayerProgress pp = of(p);
        boolean changed = false;
        for (Entry q : BookData.server().quests) {
            if (!type.equals(q.type) || q.targets.isEmpty() || pp.done.contains(q.id)) continue;
            int[] c = counts(pp, q);
            boolean touched = false;
            for (int i = 0; i < q.targets.size(); i++) {
                String t = q.targets.get(i);
                if (Entry.idOf(t).equals(id)) {
                    int nv = Math.min(Entry.countOf(t), c[i] + amount);
                    if (nv != c[i]) {
                        c[i] = nv;
                        touched = true;
                    }
                }
            }
            if (touched) {
                changed = true;
                if (complete(q, c)) finish(p, pp, q);
            }
        }
        return changed;
    }

    /** Se llama cada segundo: las misiones de "obtener" se cumplen si el jugador tiene los objetos. */
    public static void checkCollect(ServerPlayer p) {
        PlayerProgress pp = of(p);
        Map<String, Integer> inv = null;
        boolean changed = false;
        for (Entry q : BookData.server().quests) {
            if (!"collect".equals(q.type) || q.targets.isEmpty() || pp.done.contains(q.id)) continue;
            if (inv == null) inv = inventoryCounts(p);
            int[] c = counts(pp, q);
            boolean touched = false;
            for (int i = 0; i < q.targets.size(); i++) {
                String t = q.targets.get(i);
                int have = Math.min(Entry.countOf(t), inv.getOrDefault(Entry.idOf(t), 0));
                if (have != c[i]) {
                    c[i] = have;
                    touched = true;
                }
            }
            if (touched) {
                changed = true;
                if (complete(q, c)) finish(p, pp, q);
            }
        }
        if (changed) Net.sync(p, false);
    }

    private static int[] counts(PlayerProgress pp, Entry q) {
        int[] c = pp.counts.get(q.id);
        if (c == null || c.length != q.targets.size()) {
            c = new int[q.targets.size()];
            pp.counts.put(q.id, c);
        }
        return c;
    }

    private static boolean complete(Entry q, int[] c) {
        for (int i = 0; i < q.targets.size(); i++) {
            if (c[i] < Entry.countOf(q.targets.get(i))) return false;
        }
        return true;
    }

    private static Map<String, Integer> inventoryCounts(ServerPlayer p) {
        Map<String, Integer> m = new HashMap<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            ResourceLocation k = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (k != null) m.merge(k.toString(), s.getCount(), Integer::sum);
        }
        return m;
    }

    private static void finish(ServerPlayer p, PlayerProgress pp, Entry q) {
        pp.done.add(q.id);
        p.sendSystemMessage(Component.literal("Mision completada: ").withStyle(ChatFormatting.GREEN)
                .append(TextFmt.parse(q.title)));
        for (String r : q.rewards) {
            ResourceLocation rl = ResourceLocation.tryParse(Entry.idOf(r));
            if (rl == null) continue;
            Item it = ForgeRegistries.ITEMS.getValue(rl);
            if (it == null || it == Items.AIR) continue;
            int n = Entry.countOf(r);
            int total = n;
            while (n > 0) {
                int k = Math.min(n, Math.max(1, it.getMaxStackSize()));
                ItemStack st = new ItemStack(it, k);
                n -= k;
                p.getInventory().add(st);
                if (!st.isEmpty()) p.drop(st, false);
            }
            p.sendSystemMessage(Component.literal("  Recompensa: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(total + "x ").withStyle(ChatFormatting.YELLOW))
                    .append(new ItemStack(it).getHoverName()));
        }
        p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        save(p.getServer());
    }
}
