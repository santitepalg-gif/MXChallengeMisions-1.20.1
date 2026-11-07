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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Panel de administracion: crear/editar misiones con objetivos y recompensas. */
public class QuestPanelScreen extends Screen {
    private static final class Sl {
        final String id;
        int n;
        final ItemStack icon;

        Sl(String id, int n, ItemStack icon) {
            this.id = id;
            this.n = n;
            this.icon = icon;
        }
    }

    private record PItem(ItemStack stack, String id, String name) {
    }

    private static List<PItem> ITEMS;
    private static List<PItem> EGGS;

    private static final String CODES = "0123456789abcdef";
    private static final String[] EMOJIS = {"corazon", "estrella", "calavera", "diamante", "chile", "cempasuchil"};
    private static final String[] KEYS = {"kill", "collect", "craft"};
    private static final int TG = 27;
    private static final int RW = 18;

    // posiciones fijas
    private static final int TG_Y = 68;
    private static final int RW_Y = 192;
    private static final int PAL_Y = 46;
    private static final int PREVIEW_Y = 152;

    private final Screen parent;
    private String type = "kill";
    private final Sl[] tg = new Sl[TG];
    private final Sl[] rw = new Sl[RW];
    private int selGrid = 0;
    private int sel = -1;
    private boolean invOnly = false;
    private boolean listMode = false;
    private int editIndex = -1;
    private int palScroll = 0;
    private int listScroll = 0;
    private List<PItem> pal = new ArrayList<>();
    private ItemStack hoverStack = null;

    private EditBox titleBox, descBox, idBox, searchBox, lastBox;
    private final Button[] typeBtns = new Button[3];
    private Button addBtn, invBtn, listBtn;

    private int lx, mx0, midW, rx, palCols, palRows;

    public QuestPanelScreen(Screen parent) {
        super(Component.literal("Panel de misiones"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static void buildStatic() {
        if (ITEMS != null) return;
        List<PItem> items = new ArrayList<>();
        List<PItem> eggs = new ArrayList<>();
        for (Item it : ForgeRegistries.ITEMS.getValues()) {
            if (it == Items.AIR) continue;
            ResourceLocation k = ForgeRegistries.ITEMS.getKey(it);
            if (k == null) continue;
            ItemStack st = new ItemStack(it);
            String name = st.getHoverName().getString().toLowerCase(Locale.ROOT);
            items.add(new PItem(st, k.toString(), name));
            if (it instanceof SpawnEggItem egg) {
                ResourceLocation ek = ForgeRegistries.ENTITY_TYPES.getKey(egg.getType(null));
                if (ek != null) eggs.add(new PItem(st, ek.toString(), name));
            }
        }
        items.sort(Comparator.comparing(PItem::id));
        eggs.sort(Comparator.comparing(PItem::id));
        ITEMS = items;
        EGGS = eggs;
    }

    @Override
    protected void init() {
        buildStatic();
        String oldTitle = titleBox != null ? titleBox.getValue() : "";
        String oldDesc = descBox != null ? descBox.getValue() : "";
        String oldSearch = searchBox != null ? searchBox.getValue() : "";

        lx = 8;
        midW = width >= 600 ? 176 : 150;
        mx0 = lx + 9 * 18 + 16;
        rx = mx0 + midW + 12;
        palCols = Math.max(4, Math.min(14, (width - rx - 8) / 18));
        palRows = Math.max(3, (height - PAL_Y - 32) / 18);

        listBtn = addRenderableWidget(Button.builder(Component.literal(listMode ? "<<" : ">>"), b -> {
            listMode = !listMode;
            b.setMessage(Component.literal(listMode ? "<<" : ">>"));
        }).bounds(3, 3, 22, 16).build());

        String[] names = {"Matar", "Obtener", "Craftear"};
        for (int i = 0; i < 3; i++) {
            final String k = KEYS[i];
            typeBtns[i] = addRenderableWidget(Button.builder(Component.literal(names[i]), b -> setType(k))
                    .bounds(lx + i * 57, 32, 54, 16).build());
        }
        refreshTypeButtons();

        idBox = new EditBox(font, lx, 128, 138, 16, Component.literal("id"));
        idBox.setMaxLength(100);
        idBox.setHint(Component.literal("minecraft:bread*3"));
        addRenderableWidget(idBox);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> addFromText())
                .bounds(lx + 142, 128, 24, 16).build());

        titleBox = new EditBox(font, mx0, 32, midW, 16, Component.literal("Titulo"));
        titleBox.setMaxLength(100);
        titleBox.setValue(oldTitle);
        addRenderableWidget(titleBox);
        descBox = new EditBox(font, mx0, 62, midW, 16, Component.literal("Descripcion"));
        descBox.setMaxLength(200);
        descBox.setValue(oldDesc);
        addRenderableWidget(descBox);
        lastBox = titleBox;

        String[][] fm = {{"N", "&l"}, {"K", "&o"}, {"S", "&n"}, {"R", "&r"}};
        for (int i = 0; i < 4; i++) {
            final String code = fm[i][1];
            addRenderableWidget(Button.builder(Component.literal(fm[i][0]), b -> insert(code))
                    .bounds(mx0 + 108 + (i % 2) * 22, 96 + (i / 2) * 13, 20, 11).build());
        }

        searchBox = new EditBox(font, rx, 8, palCols * 18, 16, Component.literal("Buscar"));
        searchBox.setMaxLength(40);
        searchBox.setHint(Component.literal("Buscar..."));
        searchBox.setValue(oldSearch);
        searchBox.setResponder(s -> {
            palScroll = 0;
            rebuildPalette();
        });
        addRenderableWidget(searchBox);

        invBtn = addRenderableWidget(Button.builder(Component.literal("Mi inventario: " + (invOnly ? "SI" : "NO")), b -> {
            invOnly = !invOnly;
            b.setMessage(Component.literal("Mi inventario: " + (invOnly ? "SI" : "NO")));
            palScroll = 0;
            rebuildPalette();
        }).bounds(rx, 28, palCols * 18, 14).build());

        addBtn = addRenderableWidget(Button.builder(
                Component.literal(editIndex >= 0 ? "Guardar cambios" : "Agregar mision"), b -> submit())
                .bounds(lx, height - 24, 110, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Limpiar"), b -> clearForm())
                .bounds(lx + 114, height - 24, 54, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cerrar"), b -> onClose())
                .bounds(mx0 + midW / 2 - 30, height - 24, 60, 18).build());

        rebuildPalette();
    }

    // ---------------------------------------------------------------- logica

    private Sl[] arr(int grid) {
        return grid == 0 ? tg : rw;
    }

    private static int count(Sl[] a) {
        int n = 0;
        for (Sl s : a) if (s != null) n++;
        return n;
    }

    private void refreshTypeButtons() {
        for (int i = 0; i < 3; i++) {
            typeBtns[i].active = !KEYS[i].equals(type);
        }
    }

    private void setType(String k) {
        if (!k.equals(type)) {
            type = k;
            Arrays.fill(tg, null);
        }
        selGrid = 0;
        sel = -1;
        palScroll = 0;
        refreshTypeButtons();
        rebuildPalette();
    }

    private void insert(String s) {
        if (lastBox == null) lastBox = titleBox;
        lastBox.insertText(s);
    }

    private void rebuildPalette() {
        boolean eggs = selGrid == 0 && type.equals("kill");
        List<PItem> base = eggs ? EGGS : ITEMS;
        String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        Set<String> have = null;
        if (invOnly && !eggs && minecraft != null && minecraft.player != null) {
            have = new HashSet<>();
            Inventory inv = minecraft.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty()) continue;
                ResourceLocation k = ForgeRegistries.ITEMS.getKey(s.getItem());
                if (k != null) have.add(k.toString());
            }
        }
        List<PItem> out = new ArrayList<>();
        for (PItem p : base) {
            if (have != null && !have.contains(p.id())) continue;
            if (!q.isEmpty() && !p.name().contains(q) && !p.id().contains(q)) continue;
            out.add(p);
        }
        pal = out;
        clampPalScroll();
    }

    private void clampPalScroll() {
        int rows = (pal.size() + palCols - 1) / palCols;
        palScroll = Mth.clamp(palScroll, 0, Math.max(0, rows - palRows));
    }

    private void place(Sl[] a, String id, int n, ItemStack icon) {
        int target = -1;
        if (sel >= 0 && sel < a.length) {
            target = sel;
        } else {
            for (int i = 0; i < a.length; i++) {
                if (a[i] != null && a[i].id.equals(id)) {
                    a[i].n = Math.min(9999, a[i].n + n);
                    return;
                }
            }
            for (int i = 0; i < a.length; i++) {
                if (a[i] == null) {
                    target = i;
                    break;
                }
            }
        }
        if (target < 0) return;
        a[target] = new Sl(id, n, icon.copy());
        sel = -1;
    }

    private void addFromPalette(PItem p) {
        place(arr(selGrid), p.id(), 1, p.stack());
    }

    private void addFromText() {
        String raw = idBox.getValue().trim();
        if (raw.isEmpty()) return;
        String id = Entry.idOf(raw).toLowerCase(Locale.ROOT);
        int n = Entry.countOf(raw);
        ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
        if (rl == null) return;
        boolean kill = selGrid == 0 && type.equals("kill");
        if (kill && !ForgeRegistries.ENTITY_TYPES.containsKey(rl)) return;
        if (!kill && (!ForgeRegistries.ITEMS.containsKey(rl) || ForgeRegistries.ITEMS.getValue(rl) == Items.AIR)) return;
        place(arr(selGrid), rl.toString(), n, Icons.stack(kill, rl));
        idBox.setValue("");
    }

    private void submit() {
        Entry e = new Entry();
        e.title = titleBox.getValue();
        e.desc = descBox.getValue();
        e.type = type;
        for (Sl s : tg) if (s != null) e.targets.add(s.id + "*" + s.n);
        for (Sl s : rw) if (s != null) e.rewards.add(s.id + "*" + s.n);
        if (e.title.isBlank()) return;
        int action = editIndex >= 0 ? EditPacket.REPLACE : EditPacket.ADD;
        Net.CHANNEL.sendToServer(EditPacket.of(action, 0, editIndex, e));
        clearForm();
    }

    private void clearForm() {
        titleBox.setValue("");
        descBox.setValue("");
        Arrays.fill(tg, null);
        Arrays.fill(rw, null);
        sel = -1;
        selGrid = 0;
        editIndex = -1;
        addBtn.setMessage(Component.literal("Agregar mision"));
        rebuildPalette();
    }

    private void loadEntry(int i) {
        List<Entry> l = ClientData.data.quests;
        if (i < 0 || i >= l.size()) return;
        Entry q = l.get(i);
        titleBox.setValue(q.title);
        descBox.setValue(q.desc);
        type = q.type;
        Arrays.fill(tg, null);
        Arrays.fill(rw, null);
        boolean kill = type.equals("kill");
        int k = 0;
        for (String t : q.targets) {
            if (k >= TG) break;
            ResourceLocation rl = ResourceLocation.tryParse(Entry.idOf(t));
            if (rl == null) continue;
            tg[k++] = new Sl(rl.toString(), Entry.countOf(t), Icons.stack(kill, rl));
        }
        k = 0;
        for (String t : q.rewards) {
            if (k >= RW) break;
            ResourceLocation rl = ResourceLocation.tryParse(Entry.idOf(t));
            if (rl == null) continue;
            rw[k++] = new Sl(rl.toString(), Entry.countOf(t), Icons.stack(false, rl));
        }
        editIndex = i;
        listMode = false;
        listBtn.setMessage(Component.literal(">>"));
        selGrid = 0;
        sel = -1;
        refreshTypeButtons();
        addBtn.setMessage(Component.literal("Guardar cambios"));
        rebuildPalette();
    }

    // ---------------------------------------------------------------- posiciones

    private int[] slotAt(double mx, double my) {
        int c = (int) Math.floor((mx - lx) / 18.0);
        if (mx >= lx && c >= 0 && c < 9) {
            int r1 = (int) Math.floor((my - TG_Y) / 18.0);
            if (my >= TG_Y && r1 >= 0 && r1 < 3) return new int[]{0, r1 * 9 + c};
            int r2 = (int) Math.floor((my - RW_Y) / 18.0);
            if (my >= RW_Y && r2 >= 0 && r2 < 2) return new int[]{1, r2 * 9 + c};
        }
        return null;
    }

    private int paletteAt(double mx, double my) {
        if (mx < rx || my < PAL_Y) return -1;
        int c = (int) ((mx - rx) / 18);
        int r = (int) ((my - PAL_Y) / 18);
        if (c >= palCols || r >= palRows) return -1;
        int idx = (palScroll + r) * palCols + c;
        return idx < pal.size() ? idx : -1;
    }

    // ---------------------------------------------------------------- render

    private void drawSlotBg(GuiGraphics g, int x, int y, boolean selected) {
        g.fill(x, y, x + 18, y + 18, selected ? 0xFFFFFF55 : 0xFF373737);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    private void drawSlot(GuiGraphics g, int x, int y, Sl s, boolean selected, int mx, int my) {
        drawSlotBg(g, x, y, selected);
        if (s != null) {
            s.icon.setCount(s.n);
            g.renderItem(s.icon, x + 1, y + 1);
            g.renderItemDecorations(font, s.icon, x + 1, y + 1);
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) hoverStack = s.icon;
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        hoverStack = null;

        // ---- columna izquierda
        g.drawString(font, "Panel de misiones", lx + 20, 8, 0xFFAA00);
        g.drawString(font, "Tipo de mision", lx, 22, 0xFFFFFF);
        String tl = type.equals("kill") ? "Mobs a matar" : type.equals("collect") ? "Objetos a obtener" : "Objetos a craftear";
        g.drawString(font, tl, lx, 56, selGrid == 0 ? 0xFFAA00 : 0x999999);
        String c1 = count(tg) + "/" + TG;
        g.drawString(font, c1, lx + 162 - font.width(c1), 56, 0xFFFFFF);
        for (int i = 0; i < TG; i++) {
            drawSlot(g, lx + (i % 9) * 18, TG_Y + (i / 9) * 18, tg[i], selGrid == 0 && sel == i, mx, my);
        }
        g.drawWordWrap(font, Component.literal("Clic en un espacio y luego en un objeto. Clic derecho: vaciar. Rueda: cantidad."),
                lx, 148, 166, 0xAAAAAA);
        g.drawString(font, "Recompensas", lx, 180, selGrid == 1 ? 0xFFAA00 : 0x999999);
        String c2 = count(rw) + "/" + RW;
        g.drawString(font, c2, lx + 162 - font.width(c2), 180, 0xFFFFFF);
        for (int i = 0; i < RW; i++) {
            drawSlot(g, lx + (i % 9) * 18, RW_Y + (i / 9) * 18, rw[i], selGrid == 1 && sel == i, mx, my);
        }

        // ---- columna central
        g.drawString(font, editIndex >= 0 ? "Editando mision #" + (editIndex + 1) : "Mision nueva", mx0, 8, 0xFFAA00);
        g.drawString(font, "Titulo", mx0, 22, 0xFFFFFF);
        g.drawString(font, "Descripcion", mx0, 52, 0xFFFFFF);
        g.drawString(font, "Colores y formato", mx0, 84, 0xFFAA00);
        for (int i = 0; i < 16; i++) {
            int x = mx0 + (i % 8) * 13;
            int y = 96 + (i / 8) * 13;
            ChatFormatting f = ChatFormatting.getByCode(CODES.charAt(i));
            Integer col = f == null ? null : f.getColor();
            g.fill(x, y, x + 11, y + 11, 0xFF000000);
            g.fill(x + 1, y + 1, x + 10, y + 10, 0xFF000000 | (col == null ? 0xFFFFFF : col));
        }
        for (int i = 0; i < EMOJIS.length; i++) {
            int x = mx0 + i * 16;
            g.fill(x, 124, x + 14, 136, 0xFF202020);
            g.drawString(font, TextFmt.glyph(EMOJIS[i]), x + 3, 126, 0xFFFFFF);
        }

        g.drawString(font, "Vista previa", mx0, 140, 0xFFFFFF);
        int ph = Math.max(40, height - 30 - PREVIEW_Y);
        g.fill(mx0, PREVIEW_Y, mx0 + midW, PREVIEW_Y + ph, 0xFF5B2A86);
        g.fill(mx0 + 1, PREVIEW_Y + 1, mx0 + midW - 1, PREVIEW_Y + ph - 1, 0xF0100010);
        g.enableScissor(mx0 + 2, PREVIEW_Y + 2, mx0 + midW - 2, PREVIEW_Y + ph - 2);
        int y = PREVIEW_Y + 5;
        String t = titleBox.getValue();
        for (FormattedCharSequence l : font.split(TextFmt.parse(t.isEmpty() ? "&7(sin titulo)" : t), midW - 10)) {
            g.drawString(font, l, mx0 + 5, y, 0xFFFFFF, false);
            y += 10;
        }
        Component d = TextFmt.parse(descBox.getValue()).append(Component.literal(" \u2713").withStyle(ChatFormatting.GREEN));
        for (FormattedCharSequence l : font.split(d, midW - 10)) {
            g.drawString(font, l, mx0 + 5, y, 0xFFFFFF, false);
            y += 10;
        }
        if (count(rw) > 0) {
            g.drawString(font, "Recompensa:", mx0 + 5, y + 2, 0xFFFF55, false);
            y += 13;
            int per = Math.max(1, (midW - 10) / 18);
            int n = 0;
            for (Sl s : rw) {
                if (s == null) continue;
                int ix = mx0 + 5 + (n % per) * 18;
                int iy = y + (n / per) * 18;
                s.icon.setCount(s.n);
                g.renderItem(s.icon, ix, iy);
                g.renderItemDecorations(font, s.icon, ix, iy);
                n++;
            }
        }
        g.disableScissor();

        // ---- columna derecha
        if (listMode) {
            drawList(g, mx, my);
        } else {
            for (int r = 0; r < palRows; r++) {
                for (int c = 0; c < palCols; c++) {
                    int idx = (palScroll + r) * palCols + c;
                    int x = rx + c * 18;
                    int yy = PAL_Y + r * 18;
                    drawSlotBg(g, x, yy, false);
                    if (idx < pal.size()) {
                        ItemStack st = pal.get(idx).stack();
                        g.renderItem(st, x + 1, yy + 1);
                        if (mx >= x && mx < x + 18 && my >= yy && my < yy + 18) hoverStack = st;
                    }
                }
            }
        }

        super.render(g, mx, my, pt);
        if (hoverStack != null) {
            g.renderTooltip(font, hoverStack, mx, my);
        }
    }

    private void drawList(GuiGraphics g, int mx, int my) {
        List<Entry> l = ClientData.data.quests;
        int w = palCols * 18;
        if (l.isEmpty()) {
            g.drawString(font, "No hay misiones.", rx, PAL_Y + 4, 0x999999);
        }
        for (int r = 0; r < palRows; r++) {
            int k = listScroll + r;
            if (k >= l.size()) break;
            Entry q = l.get(k);
            int y = PAL_Y + r * 18;
            g.fill(rx, y, rx + w, y + 16, 0xFF202020);
            String name = (k + 1) + ". " + TextFmt.strip(q.title);
            g.drawString(font, font.plainSubstrByWidth(name, w - 48), rx + 3, y + 4, 0xFFFFFF, false);
            boolean he = mx >= rx + w - 40 && mx < rx + w - 21 && my >= y && my < y + 16;
            boolean hx = mx >= rx + w - 20 && mx < rx + w && my >= y && my < y + 16;
            g.drawString(font, "[E]", rx + w - 38, y + 4, he ? 0xFFFF55 : 0xFFAA00, false);
            g.drawString(font, "[X]", rx + w - 18, y + 4, hx ? 0xFF5555 : 0xAA0000, false);
        }
    }

    // ---------------------------------------------------------------- entrada

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (titleBox.isMouseOver(mx, my)) lastBox = titleBox;
        else if (descBox.isMouseOver(mx, my)) lastBox = descBox;

        int[] h = slotAt(mx, my);
        if (h != null) {
            setFocused(null);
            if (btn == 1) {
                arr(h[0])[h[1]] = null;
                sel = -1;
            } else if (btn == 0) {
                boolean changed = selGrid != h[0];
                selGrid = h[0];
                sel = h[1];
                if (changed) {
                    palScroll = 0;
                    rebuildPalette();
                }
            }
            return true;
        }

        if (btn == 0 && !listMode) {
            int idx = paletteAt(mx, my);
            if (idx >= 0) {
                setFocused(null);
                addFromPalette(pal.get(idx));
                return true;
            }
        }

        if (btn == 0 && listMode && mx >= rx && my >= PAL_Y) {
            int r = (int) ((my - PAL_Y) / 18);
            int k = listScroll + r;
            List<Entry> l = ClientData.data.quests;
            if (r < palRows && k < l.size()) {
                double dx = mx - rx;
                int w = palCols * 18;
                if (dx >= w - 20 && dx < w) {
                    Net.CHANNEL.sendToServer(EditPacket.of(EditPacket.REMOVE, 0, k, null));
                    if (editIndex == k) clearForm();
                    else if (editIndex > k) editIndex--;
                } else if (dx >= w - 40 && dx < w - 20) {
                    loadEntry(k);
                }
                return true;
            }
        }

        if (btn == 0) {
            for (int i = 0; i < 16; i++) {
                int x = mx0 + (i % 8) * 13;
                int y = 96 + (i / 8) * 13;
                if (mx >= x && mx < x + 11 && my >= y && my < y + 11) {
                    insert("&" + CODES.charAt(i));
                    return true;
                }
            }
            for (int i = 0; i < EMOJIS.length; i++) {
                int x = mx0 + i * 16;
                if (mx >= x && mx < x + 14 && my >= 124 && my < 136) {
                    insert(":" + EMOJIS[i] + ":");
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int[] h = slotAt(mx, my);
        if (h != null) {
            Sl s = arr(h[0])[h[1]];
            if (s != null) {
                int step = hasShiftDown() ? 10 : 1;
                s.n = Mth.clamp(s.n + (delta > 0 ? step : -step), 1, 9999);
            }
            return true;
        }
        if (mx >= rx && my >= PAL_Y) {
            int dir = delta > 0 ? -1 : 1;
            if (listMode) {
                listScroll = Mth.clamp(listScroll + dir, 0, Math.max(0, ClientData.data.quests.size() - palRows));
            } else {
                palScroll += dir;
                clampPalScroll();
            }
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }
}
