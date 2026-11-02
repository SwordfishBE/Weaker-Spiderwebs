package net.weaker_spiderwebs;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.TitleScreen;

/** This entrypoint must load without linking any optional Cloth Config classes. */
public class MissingClothClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var screen = new WeakerSpiderwebsModMenu().getModConfigScreenFactory().create(new TitleScreen());
            if (!(screen instanceof AlertScreen)) throw new AssertionError("Missing Cloth Config must show a notice");
            client.setScreenAndShow(screen);
        });
        context.waitTicks(3);
        context.takeScreenshot("weaker-spiderwebs-missing-cloth");
        context.setScreen(TitleScreen::new);
    }
}
