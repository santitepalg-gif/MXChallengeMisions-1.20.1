package com.libromx.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public class TextFmt {
    public static final ResourceLocation EMOJI_FONT = new ResourceLocation("libromx", "emoji");
    public static final Map<String, Character> EMOJI = new LinkedHashMap<>();

    static {
        EMOJI.put("corazon", '\uE000');
        EMOJI.put("estrella", '\uE001');
        EMOJI.put("calavera", '\uE002');
        EMOJI.put("diamante", '\uE003');
        EMOJI.put("chile", '\uE004');
        EMOJI.put("cempasuchil", '\uE005');
    }

    public static Component glyph(String name) {
        Character c = EMOJI.get(name);
        if (c == null) return Component.empty();
        return Component.literal(String.valueOf(c)).withStyle(Style.EMPTY.withFont(EMOJI_FONT));
    }

    /** Convierte codigos &a, &l, &#RRGGBB y :emoji: en un Component. */
    public static MutableComponent parse(String s) {
        MutableComponent root = Component.empty();
        if (s == null) return root;
        StringBuilder buf = new StringBuilder();
        Style style = Style.EMPTY;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == '#' && i + 8 <= s.length() && s.substring(i + 2, i + 8).matches("[0-9a-fA-F]{6}")) {
                    flush(root, buf, style);
                    style = style.withColor(TextColor.fromRgb(Integer.parseInt(s.substring(i + 2, i + 8), 16)));
                    i += 8;
                    continue;
                }
                ChatFormatting f = ChatFormatting.getByCode(Character.toLowerCase(n));
                if (f != null) {
                    flush(root, buf, style);
                    style = style.applyLegacyFormat(f);
                    i += 2;
                    continue;
                }
            }
            if (c == ':') {
                int j = s.indexOf(':', i + 1);
                if (j > i + 1) {
                    String name = s.substring(i + 1, j);
                    if (EMOJI.containsKey(name)) {
                        flush(root, buf, style);
                        root.append(glyph(name));
                        i = j + 1;
                        continue;
                    }
                }
            }
            buf.append(c);
            i++;
        }
        flush(root, buf, style);
        return root;
    }

    private static void flush(MutableComponent root, StringBuilder buf, Style style) {
        if (buf.length() > 0) {
            root.append(Component.literal(buf.toString()).withStyle(style));
            buf.setLength(0);
        }
    }

    /** Quita codigos de color y emojis (texto plano). */
    public static String strip(String s) {
        if (s == null) return "";
        String r = s.replaceAll("&#[0-9a-fA-F]{6}", "").replaceAll("&[0-9a-fk-orA-FK-OR]", "");
        for (String k : EMOJI.keySet()) {
            r = r.replace(":" + k + ":", "");
        }
        return r.trim();
    }
}
