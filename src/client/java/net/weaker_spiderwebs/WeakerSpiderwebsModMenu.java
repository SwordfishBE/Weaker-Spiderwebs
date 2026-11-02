package net.weaker_spiderwebs;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.network.chat.Component;

public final class WeakerSpiderwebsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
            return parent -> new AlertScreen(() -> Minecraft.getInstance().setScreenAndShow(parent),
                    Component.literal("Weaker Spiderwebs"), Component.literal(
                    "[Weaker Spiderwebs] Install Cloth Config for Minecraft 26.4 to edit the configuration here. "
                            + "You can also edit config/weaker_spiderwebs.json."));
        }
        if (!hasCompatibleClothConfig()) {
            return parent -> new AlertScreen(() -> Minecraft.getInstance().setScreenAndShow(parent),
                    Component.literal("Weaker Spiderwebs"), Component.literal(
                    "[Weaker Spiderwebs] This menu requires Cloth Config for Minecraft 26.4. "
                            + "The installed older version cannot render this screen safely. "
                            + "Until it is updated, edit config/weaker_spiderwebs.json and use /weaker_spiderwebs reload."));
        }
        return WeakerSpiderwebsConfigScreen::create;
    }

    static boolean hasCompatibleClothConfig() {
        var cloth = FabricLoader.getInstance().getModContainer("cloth-config");
        if (cloth.isEmpty()) return false;
        try {
            return net.fabricmc.loader.api.metadata.version.VersionPredicate.parse(">=26.4-")
                    .test(cloth.get().getMetadata().getVersion());
        } catch (net.fabricmc.loader.api.VersionParsingException exception) {
            return false;
        }
    }
}
