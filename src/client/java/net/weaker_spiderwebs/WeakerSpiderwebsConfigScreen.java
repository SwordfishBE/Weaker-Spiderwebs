package net.weaker_spiderwebs;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class WeakerSpiderwebsConfigScreen {
    private WeakerSpiderwebsConfigScreen() {}

    static Screen create(Screen parent) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null && !minecraft.hasSingleplayerServer()) {
            return notice(parent, "[Weaker Spiderwebs] Multiplayer uses the server configuration. "
                    + "Edit the server file and use /weaker_spiderwebs reload there.");
        }
        final int initial;
        try {
            initial = WeakerSpiderwebs.delayForEditing();
        } catch (java.io.IOException | RuntimeException exception) {
            return notice(parent, "[Weaker Spiderwebs] Cannot open config. The existing file has been preserved. "
                    + "Fix config/weaker_spiderwebs.json first.");
        }
        int[] draft = {initial};
        var builder = ConfigBuilder.create().setParentScreen(parent)
                .setTitle(Component.literal("Weaker Spiderwebs Config"));
        builder.getOrCreateCategory(Component.literal("General")).addEntry(builder.entryBuilder()
                .startIntField(Component.literal("Web breaking delay (ms)"), initial)
                .setDefaultValue(500).setMin(0).setMax(10000)
                .setTooltip(Component.literal("Delay after detecting a player in a cobweb. Range: 0-10000 ms."),
                        Component.literal("Applies immediately in singleplayer. Server ticks round the delay up to 50 ms."))
                .setSaveConsumer(value -> draft[0] = value).build());
        builder.setSavingRunnable(() -> {
            var server = minecraft.getSingleplayerServer();
            if (minecraft.getConnection() != null && server == null) return;
            WeakerSpiderwebs.saveEditedDelay(draft[0], server, message -> {
                // Defer until Cloth Config has closed the editor, including for a title-screen save.
                minecraft.schedule(() -> minecraft.setScreenAndShow(notice(parent, message.getString())));
            });
        });
        return builder.build();
    }

    private static Screen notice(Screen parent, String message) {
        return new AlertScreen(() -> Minecraft.getInstance().setScreenAndShow(parent),
                Component.literal("Weaker Spiderwebs"), Component.literal(message));
    }
}
