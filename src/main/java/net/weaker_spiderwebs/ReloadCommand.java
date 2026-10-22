package net.weaker_spiderwebs;

import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

final class ReloadCommand {
    private ReloadCommand() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, Path path,
                         BiConsumer<MinecraftServer, ModConfig> apply) {
        dispatcher.register(Commands.literal(WeakerSpiderwebs.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("reload").executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ModConfig config;
                    try {
                        config = ModConfig.read(path);
                    } catch (IOException | RuntimeException exception) {
                        source.sendFailure(Component.literal(WeakerSpiderwebs.PREFIX
                                + "Config reload failed. The current configuration remains active. "
                                + "Check config/weaker_spiderwebs.json."));
                        WeakerSpiderwebs.LOGGER.warn(WeakerSpiderwebs.PREFIX
                                + "Could not reload config {}; keeping the current configuration.", path, exception);
                        return 0;
                    }
                    apply.accept(source.getServer(), config);
                    source.sendSuccess(() -> Component.literal(WeakerSpiderwebs.PREFIX
                            + "Config reloaded. breakSpiderwebDelay: "
                            + config.breakSpiderwebDelay() + " ms."), false);
                    return 1;
                })));
    }
}
