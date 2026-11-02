package net.weaker_spiderwebs;

import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public class ConfigMenuClientTest implements FabricClientGameTest {
    private ClothConfigScreen editor;

    private void open(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var screen = new WeakerSpiderwebsModMenu().getModConfigScreenFactory().create(new TitleScreen());
            if (!WeakerSpiderwebsModMenu.hasCompatibleClothConfig()) {
                if (!(screen instanceof AlertScreen)) throw new AssertionError("Old Cloth must show a safe warning");
                client.setScreenAndShow(screen);
                // Exercise future editor data/callbacks without rendering incompatible 26.3 widgets.
                editor = (ClothConfigScreen) WeakerSpiderwebsConfigScreen.create(new TitleScreen());
            } else {
                if (!(screen instanceof ClothConfigScreen cloth)) throw new AssertionError("Expected a Cloth editor");
                editor = cloth;
                client.setScreenAndShow(editor);
            }
        });
        context.waitTicks(3);
    }

    private IntegerListEntry entry() {
        return (IntegerListEntry) editor.getCategorizedEntries().values().iterator().next().getFirst();
    }

    private void save(ClientGameTestContext context, int delay) {
        open(context);
        context.runOnClient(client -> {
            entry().setValue(Integer.toString(delay));
            editor.saveAll(true);
        });
        context.waitTicks(10);
        context.runOnClient(client -> {
            try {
                if (WeakerSpiderwebs.delayForEditing() != delay) throw new AssertionError("GUI save did not persist delay");
            } catch (java.io.IOException exception) { throw new RuntimeException(exception); }
        });
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        var path = FabricLoader.getInstance().getConfigDir().resolve("weaker_spiderwebs.json");
        try {
            byte[] original = Files.exists(path) ? Files.readAllBytes(path) : null;
            try {
                // Deterministic fixture; restored even if the test fails.
                ModConfig.save(path, 500);
                open(context);
                context.takeScreenshot("weaker-spiderwebs-old-cloth-warning");
                context.runOnClient(client -> {
                    entry().setValue("-1");
                    if (entry().getConfigError().isEmpty()) throw new AssertionError("Negative delay must be rejected");
                    entry().setValue("10001");
                    if (entry().getConfigError().isEmpty()) throw new AssertionError("Excessive delay must be rejected");
                    entry().setValue("1234");
                });
                context.setScreen(TitleScreen::new);
                if (ModConfig.read(path).breakSpiderwebDelay() != 500) throw new AssertionError("Cancelled edit changed config");
                save(context, 10000);
                context.setScreen(TitleScreen::new);
                try (var world = context.worldBuilder().create()) {
                    AtomicReference<BlockPos> web = new AtomicReference<>();
                    world.getServer().runOnServer(server -> {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        var pos = player.blockPosition().above(3);
                        player.level().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
                        player.level().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
                        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        web.set(pos);
                        player.level().setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
                    });
                    context.waitTicks(15);
                    world.getServer().runOnServer(server -> {
                        if (!server.overworld().getBlockState(web.get()).is(Blocks.COBWEB))
                            throw new AssertionError("Title-screen 10000 ms delay did not apply to the integrated world");
                    });
                    save(context, 0);
                    context.setScreen(() -> null);
                    context.waitTicks(15);
                    world.getServer().runOnServer(server -> {
                        if (!server.overworld().getBlockState(web.get()).isAir())
                            throw new AssertionError("Live GUI save did not reset countdowns and apply zero delay; player="
                                    + server.getPlayerList().getPlayers().getFirst().position() + "; web=" + web.get());
                    });
                }
                context.setScreen(TitleScreen::new);
            } finally {
                if (original == null) Files.deleteIfExists(path);
                else Files.write(path, original);
            }
        } catch (java.io.IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
