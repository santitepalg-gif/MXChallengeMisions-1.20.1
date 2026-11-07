package com.libromx.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Entry {
    public String id = "";
    public String title = "";
    public String desc = "";
    public String type = "kill";
    public String target = "";          // campo viejo (compatibilidad con libromx.json anterior)
    public String base = "normal";      // dificultad base (solo en dificultades)
    public List<String> targets = new ArrayList<>();   // "modid:id*cantidad"
    public List<String> rewards = new ArrayList<>();   // "modid:item*cantidad"

    public Entry() {
    }

    public Entry(String title, String desc, String type, String target, String base) {
        this.id = newId();
        this.title = title;
        this.desc = desc;
        this.type = type;
        this.base = base;
        if (target != null && !target.isBlank()) {
            this.targets.add(target);
        }
    }

    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public void fix() {
        if (id == null || id.isBlank()) id = newId();
        if (title == null) title = "";
        if (desc == null) desc = "";
        if (type == null) type = "kill";
        if (base == null) base = "normal";
        if (targets == null) targets = new ArrayList<>();
        if (rewards == null) rewards = new ArrayList<>();
        if (target != null && !target.isBlank() && targets.isEmpty()) {
            targets.add(target);
        }
        target = "";
    }

    public static String idOf(String s) {
        if (s == null) return "";
        int i = s.indexOf('*');
        return (i < 0 ? s : s.substring(0, i)).trim();
    }

    public static int countOf(String s) {
        if (s == null) return 1;
        int i = s.indexOf('*');
        if (i < 0) return 1;
        try {
            return Math.max(1, Math.min(9999, Integer.parseInt(s.substring(i + 1).trim())));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
