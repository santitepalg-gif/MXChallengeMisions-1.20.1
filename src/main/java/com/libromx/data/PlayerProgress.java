package com.libromx.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerProgress {
    public Map<String, int[]> counts = new HashMap<>();
    public Set<String> done = new HashSet<>();

    public void fix() {
        if (counts == null) counts = new HashMap<>();
        if (done == null) done = new HashSet<>();
    }

    public static PlayerProgress fromJson(String json) {
        try {
            PlayerProgress p = BookData.NET_GSON.fromJson(json, PlayerProgress.class);
            if (p == null) p = new PlayerProgress();
            p.fix();
            return p;
        } catch (Exception e) {
            return new PlayerProgress();
        }
    }
}
