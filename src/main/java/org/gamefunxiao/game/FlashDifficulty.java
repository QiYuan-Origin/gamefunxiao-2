package org.gamefunxiao.game;

public enum FlashDifficulty {
    NORMAL("normal", "正常"),
    EASY("easy", "简单"),
    FLASH_SMP("flash_smp", "FlashSMP");

    private final String id;
    private final String displayName;

    FlashDifficulty(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
