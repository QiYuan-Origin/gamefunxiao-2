package org.brickguard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private Text() {
    }

    static Component c(String text) {
        return LEGACY.deserialize(text == null ? "" : text).decoration(TextDecoration.ITALIC, false);
    }
}
