package org.brickguard;

import java.util.ArrayList;
import java.util.List;

final class MapData {
    boolean enabled;
    Point lobbySpawn;
    Point brickSpawn;
    Point netherSpawn;
    Point brickCore;
    Point brickPortal;
    Point netherPortal;
    Point obsidianPool;
    final List<Point> brickTraders = new ArrayList<>();
    final List<Point> netherTraders = new ArrayList<>();
    final List<Point> brickGuards = new ArrayList<>();
    final List<Point> netherGuards = new ArrayList<>();
    final List<Point> brickMines = new ArrayList<>();
    final List<Point> netherMines = new ArrayList<>();

    List<String> missing() {
        List<String> missing = new ArrayList<>();
        if (lobbySpawn == null) missing.add("等待大厅出生点");
        if (brickSpawn == null) missing.add("板砖出生点");
        if (netherSpawn == null) missing.add("下界出生点");
        if (brickCore == null) missing.add("板砖核心");
        if (brickPortal == null) missing.add("板砖门位置");
        if (netherPortal == null) missing.add("下界门位置");
        if (brickTraders.isEmpty()) missing.add("板砖商人");
        if (netherTraders.isEmpty()) missing.add("下界商人");
        return missing;
    }

    boolean ready() {
        return enabled && missing().isEmpty();
    }
}
