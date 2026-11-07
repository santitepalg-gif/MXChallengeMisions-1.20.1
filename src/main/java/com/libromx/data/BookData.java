package com.libromx.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.libromx.LibroMX;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BookData {
    public static final int MAX_PER_BOOK = 60;
    public static final Gson FILE_GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Gson NET_GSON = new Gson();

    public List<Entry> quests = new ArrayList<>();
    public List<Entry> notices = new ArrayList<>();
    public List<Entry> difficulties = new ArrayList<>();
    public String currentDifficulty = "";

    private static BookData server = new BookData();

    public static BookData server() {
        return server;
    }

    /** 0 = misiones, 1 = notificaciones, 2 = dificultades */
    public List<Entry> list(int book) {
        return book == 0 ? quests : book == 1 ? notices : difficulties;
    }

    public String toJson() {
        return NET_GSON.toJson(this);
    }

    public static BookData fromJson(String json) {
        try {
            BookData d = NET_GSON.fromJson(json, BookData.class);
            if (d == null) d = new BookData();
            d.fixNulls();
            return d;
        } catch (Exception e) {
            return new BookData();
        }
    }

    private void fixNulls() {
        if (quests == null) quests = new ArrayList<>();
        if (notices == null) notices = new ArrayList<>();
        if (difficulties == null) difficulties = new ArrayList<>();
        if (currentDifficulty == null) currentDifficulty = "";
        for (int b = 0; b < 3; b++) {
            list(b).removeIf(e -> e == null);
            for (Entry e : list(b)) e.fix();
        }
    }

    public static Path path(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("libromx.json");
    }

    public static void load(MinecraftServer s) {
        Path p = path(s);
        try {
            if (Files.exists(p)) {
                server = fromJson(Files.readString(p));
            } else {
                server = defaults();
                save(s);
            }
        } catch (Exception e) {
            LibroMX.LOGGER.error("No se pudo leer libromx.json", e);
            server = defaults();
        }
    }

    public static void save(MinecraftServer s) {
        try {
            Files.writeString(path(s), FILE_GSON.toJson(server));
        } catch (IOException e) {
            LibroMX.LOGGER.error("No se pudo guardar libromx.json", e);
        }
    }

    private static BookData defaults() {
        BookData d = new BookData();

        Entry q1 = new Entry("&a&lCaza de creepers", "Elimina 5 creepers :calavera: y gana diamantes :diamante:", "kill", "minecraft:creeper*5", "normal");
        q1.rewards.add("minecraft:diamond*2");
        Entry q2 = new Entry("&6Prepara tamales", "Consigue pan para la cena. :chile:", "collect", "minecraft:bread*3", "normal");
        q2.rewards.add("minecraft:cooked_beef*8");
        Entry q3 = new Entry("&cOfrenda de Dia de Muertos", "Craftea un pastel para el altar. :cempasuchil: :corazon:", "craft", "minecraft:cake*1", "normal");
        q3.rewards.add("minecraft:golden_apple*1");
        d.quests.add(q1);
        d.quests.add(q2);
        d.quests.add(q3);

        d.notices.add(new Entry("&b&lBienvenido, raza!", "Abre el libro con la tecla G. :estrella:", "kill", "", "normal"));
        d.notices.add(new Entry("&e&lLoteria esta noche", "A las 8 pm en la plaza. &dHabra premios :chile:", "kill", "", "normal"));

        Entry easy = new Entry("&aFacil: Domingo en Coyoacan", "Mobs tranquilos y comida de sobra.", "kill", "", "easy");
        Entry normal = new Entry("&6Normal: Tianguis con prisa", "El reto de siempre.", "kill", "", "normal");
        Entry hard = new Entry("&c&lPesadilla: Noche de Chupacabras", "Mobs mas fuertes. :calavera:", "kill", "", "hard");
        d.difficulties.add(easy);
        d.difficulties.add(normal);
        d.difficulties.add(hard);
        d.currentDifficulty = normal.id;
        return d;
    }
}
