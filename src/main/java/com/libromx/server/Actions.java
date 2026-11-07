package com.libromx.server;

import com.libromx.data.BookData;
import com.libromx.data.Entry;
import com.libromx.net.Net;
import com.libromx.util.TextFmt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;

public class Actions {
    public static final List<String> TYPES = List.of("kill", "collect", "craft");
    public static final List<String> BASES = List.of("peaceful", "easy", "normal", "hard");
    public static final int MAX_TARGETS = 27;
    public static final int MAX_REWARDS = 18;

    private static String clip(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** Valida y limpia lo que manda el cliente. Siempre genera un id nuevo. */
    public static Entry clean(Entry e, int book) {
        Entry c = new Entry();
        c.id = Entry.newId();
        c.title = clip(e.title, 100);
        c.desc = clip(e.desc, 200);
        if (book == 2) {
            String b = e.base == null ? "normal" : e.base.toLowerCase(Locale.ROOT);
            c.base = BASES.contains(b) ? b : "normal";
        }
        if (book == 0) {
            String t = e.type == null ? "kill" : e.type.toLowerCase(Locale.ROOT);
            c.type = TYPES.contains(t) ? t : "kill";
            if (e.targets != null) {
                for (String s : e.targets) {
                    if (c.targets.size() >= MAX_TARGETS) break;
                    String v = norm(s, c.type.equals("kill"));
                    if (v != null) c.targets.add(v);
                }
            }
            if (e.rewards != null) {
                for (String s : e.rewards) {
                    if (c.rewards.size() >= MAX_REWARDS) break;
                    String v = norm(s, false);
                    if (v != null) c.rewards.add(v);
                }
            }
        }
        return c;
    }

    private static String norm(String s, boolean entity) {
        if (s == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(Entry.idOf(s).toLowerCase(Locale.ROOT));
        if (rl == null) return null;
        boolean ok;
        if (entity) {
            ok = ForgeRegistries.ENTITY_TYPES.containsKey(rl);
        } else {
            ok = ForgeRegistries.ITEMS.containsKey(rl) && ForgeRegistries.ITEMS.getValue(rl) != Items.AIR;
        }
        return ok ? rl + "*" + Entry.countOf(s) : null;
    }

    private static void changed(MinecraftServer s) {
        BookData.save(s);
        Net.syncAll(s);
    }

    public static boolean add(MinecraftServer s, int book, Entry e) {
        if (book < 0 || book > 2) return false;
        Entry c = clean(e, book);
        if (c.title.isBlank()) return false;
        List<Entry> l = BookData.server().list(book);
        if (l.size() >= BookData.MAX_PER_BOOK) return false;
        l.add(c);
        if (book == 1) {
            s.getPlayerList().broadcastSystemMessage(
                    Component.literal("[Aviso] ").withStyle(ChatFormatting.GOLD).append(TextFmt.parse(c.title)), false);
            if (!c.desc.isEmpty()) {
                s.getPlayerList().broadcastSystemMessage(TextFmt.parse(c.desc), false);
            }
        }
        changed(s);
        return true;
    }

    public static boolean replace(MinecraftServer s, int book, int idx, Entry e) {
        if (book < 0 || book > 2) return false;
        List<Entry> l = BookData.server().list(book);
        if (idx < 0 || idx >= l.size()) return false;
        Entry c = clean(e, book);
        if (c.title.isBlank()) return false;
        l.set(idx, c);
        changed(s);
        return true;
    }

    public static boolean remove(MinecraftServer s, int book, int idx) {
        if (book < 0 || book > 2) return false;
        List<Entry> l = BookData.server().list(book);
        if (idx < 0 || idx >= l.size()) return false;
        l.remove(idx);
        changed(s);
        return true;
    }

    public static boolean addReward(MinecraftServer s, int idx, ResourceLocation item, int count) {
        List<Entry> l = BookData.server().quests;
        if (idx < 0 || idx >= l.size()) return false;
        Entry q = l.get(idx);
        if (q.rewards.size() >= MAX_REWARDS) return false;
        String v = norm(item + "*" + count, false);
        if (v == null) return false;
        q.rewards.add(v);
        changed(s);
        return true;
    }

    public static boolean applyDifficulty(MinecraftServer s, int idx) {
        List<Entry> l = BookData.server().difficulties;
        if (idx < 0 || idx >= l.size()) return false;
        Entry d = l.get(idx);
        BookData.server().currentDifficulty = d.id;
        s.setDifficulty(toDifficulty(d.base), true);
        s.getPlayerList().broadcastSystemMessage(
                Component.literal("[Dificultad] ").withStyle(ChatFormatting.RED).append(TextFmt.parse(d.title)), false);
        changed(s);
        return true;
    }

    private static Difficulty toDifficulty(String b) {
        String v = b == null ? "" : b;
        return switch (v) {
            case "peaceful" -> Difficulty.PEACEFUL;
            case "easy" -> Difficulty.EASY;
            case "hard" -> Difficulty.HARD;
            default -> Difficulty.NORMAL;
        };
    }
}
