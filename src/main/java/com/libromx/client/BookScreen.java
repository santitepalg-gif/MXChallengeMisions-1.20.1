package com.libromx.client;

import com.libromx.data.Entry;
import com.libromx.net.EditPacket;
import com.libromx.net.Net;
import com.libromx.util.TextFmt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BookScreen extends Screen {
    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private int tab = 0;
    private int scroll = 0;
    private int maxScroll = 0;
    private int px, py, pw, ph, cx, cy, cw, ch;
    private EditBox noticeBox;
    private ItemStack hoverStack = null;

    public BookScreen() {
        super(Component.literal("Libro MX"));
    }

    public void onData() {
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 16, 340);
        ph = Math.min(height - 12, 260);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx = px + 8;
        cy = py + 30;
        cw = pw - 16;
        ch = ph - 30 - 30;

        String[] names = {"Misiones", "Notificaciones", "Dificultad"};
        int tw = (pw - 24) / 3;
        for (int i = 0; i < 3; i++) {
            final int t = i;
            Button tb = Button.builder(Component.literal(names[i]), x -> {
                tab = t;
                scroll = 0;
                rebuildWidgets();
            }).bounds(px + 8 + i * (tw + 4), py + 8, tw, 16).build();
            tb.active = tab != i;
            addRenderableWidget(tb);
        }

        String old = noticeBox != null ? noticeBox.getValue() : "";
        noticeBox = null;
        if (ClientData.admin) {
            addRenderableWidget(Button.builder(Component.literal("Panel de misiones"),
                    x -> minecraft.setScreen(new QuestPanelScreen(this))).bounds(px + 8, py + ph - 24, 104, 18).build());
            if (tab == 1) {
                noticeBox = new EditBox(font, px + 118, py + ph - 24, pw - 118 - 74, 18, Component.literal("Aviso"));
                noticeBox.setMaxLength(100);
                noticeBox.setHint(Component.literal("Texto del aviso..."));
                noticeBox.setValue(old);
                addRenderableWidget(noticeBox);
                addRenderableWidget(Button.builder(Component.literal("Enviar"), x -> {
                    String s = noticeBox.getValue().trim();
                    if (s.isEmpty()) return;
                    Entry e = new Entry();
                    e.title = s;
                    Net.CHANNEL.sendToServer(EditPacket.of(EditPacket.ADD, 1, -1, e));
                    noticeBox.setValue("");
                }).bounds(px + pw - 64, py + ph - 24, 56, 18).build());
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xFF5B2A86);
        g.fill(px, py, px + pw, py + ph, 0xF0100010);
        hits.clear();
        hoverStack = null;

        g.enableScissor(cx, cy, cx + cw, cy + ch);
        int y = cy - scroll;
        List<Entry> list = ClientData.data.list(tab);
        if (list.isEmpty()) {
            g.drawString(font, "No hay nada por aqui todavia.", cx + 4, y + 4, 0x999999, false);
            y += 16;
        }
        for (int i = 0; i < list.size(); i++) {
            y = drawEntry(g, list.get(i), i, cx, y, cw, mx, my);
        }
        int total = y + scroll - cy;
        g.disableScissor();
        maxScroll = Math.max(0, total - ch);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        super.render(g, mx, my, pt);
        if (hoverStack != null) {
            g.renderTooltip(font, hoverStack, mx, my);
        }
    }

    private int drawEntry(GuiGraphics g, Entry e, int idx, int x, int y, int w, int mx, int my) {
        int top = y;
        y += 3;
        boolean quest = tab == 0;
        boolean done = quest && ClientData.progress.done.contains(e.id);

        MutableComponent title = TextFmt.parse(e.title);
        if (done) title.append(Component.literal(" \u2713").withStyle(ChatFormatting.GREEN));
        if (tab == 2 && e.id.equals(ClientData.data.currentDifficulty)) {
            title.append(Component.literal("  (actual)").withStyle(ChatFormatting.GREEN));
        }
        int tw = ClientData.admin && tab != 0 ? w - 90 : w - 8;
        for (FormattedCharSequence l : font.split(title, tw)) {
            g.drawString(font, l, x + 4, y, 0xFFFFFF, false);
            y += 10;
        }
        if (!e.desc.isEmpty()) {
            for (FormattedCharSequence l : font.split(TextFmt.parse(e.desc), w - 12)) {
                g.drawString(font, l, x + 8, y, 0xCCCCCC, false);
                y += 10;
            }
        }

        if (quest && !e.targets.isEmpty()) {
            int[] c = ClientData.progress.counts.get(e.id);
            int have = 0;
            int need = 0;
            for (int k = 0; k < e.targets.size(); k++) {
                need += Entry.countOf(e.targets.get(k));
                have += (c != null && k < c.length) ? c[k] : 0;
            }
            if (done) have = need;
            String verb = e.type.equals("kill") ? "Matar" : e.type.equals("collect") ? "Obtener" : "Craftear";
            g.drawString(font, verb + ": " + have + "/" + need, x + 8, y, done ? 0x55FF55 : 0xFFAA00, false);
            y += 10;
            y = iconRow(g, e.targets, e.type.equals("kill"), x, y, w, mx, my);
        }
        if (quest && !e.rewards.isEmpty()) {
            g.drawString(font, "Recompensa:", x + 8, y, 0xFFFF55, false);
            y += 10;
            y = iconRow(g, e.rewards, false, x, y, w, mx, my);
        }

        if (ClientData.admin && tab != 0) {
            final int bk = tab;
            final int i = idx;
            int bx = x + w - 20;
            boolean hx = mx >= bx && mx < bx + 16 && my >= top && my < top + 14;
            g.drawString(font, "[X]", bx, top + 3, hx ? 0xFF5555 : 0xAA0000, false);
            hits.add(new Hit(bx, top + 1, bx + 18, top + 13,
                    () -> Net.CHANNEL.sendToServer(EditPacket.of(EditPacket.REMOVE, bk, i, null))));
            if (tab == 2) {
                int ax = x + w - 72;
                boolean ha = mx >= ax && mx < ax + 48 && my >= top && my < top + 14;
                g.drawString(font, "[Aplicar]", ax, top + 3, ha ? 0xFFFF55 : 0xFFAA00, false);
                hits.add(new Hit(ax, top + 1, ax + 50, top + 13,
                        () -> Net.CHANNEL.sendToServer(EditPacket.of(EditPacket.APPLY, 2, i, null))));
            }
        }

        g.fill(x, y + 2, x + w, y + 3, 0x33FFFFFF);
        return y + 6;
    }

    private int iconRow(GuiGraphics g, List<String> list, boolean entity, int x, int y, int w, int mx, int my) {
        int per = Math.max(1, (w - 16) / 18);
        int n = 0;
        for (String t : list) {
            ResourceLocation rl = ResourceLocation.tryParse(Entry.idOf(t));
            if (rl == null) continue;
            ItemStack st = Icons.stack(entity, rl);
            st.setCount(Entry.countOf(t));
            int ix = x + 8 + (n % per) * 18;
            int iy = y + (n / per) * 18;
            g.renderItem(st, ix, iy);
            g.renderItemDecorations(font, st, ix, iy);
            if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16 && my >= cy && my < cy + ch) {
                hoverStack = st;
            }
            n++;
        }
        int rows = (n + per - 1) / per;
        return y + rows * 18;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && my >= cy && my < cy + ch) {
            for (Hit h : hits) {
                if (mx >= h.x1() && mx < h.x2() && my >= h.y1() && my < h.y2()) {
                    h.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Mth.clamp(scroll - (int) (delta * 18), 0, maxScroll);
        return true;
    }
}
