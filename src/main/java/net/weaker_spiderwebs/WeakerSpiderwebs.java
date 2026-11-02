package net.weaker_spiderwebs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WeakerSpiderwebs implements ModInitializer {
    public static final String MOD_ID = "weaker_spiderwebs";
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    static final String PREFIX = "[Weaker Spiderwebs] ";

    private static java.nio.file.Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
    }

    public static int delayForEditing() throws java.io.IOException {
        var path = configPath();
        return java.nio.file.Files.notExists(path) ? ModConfig.DEFAULT_DELAY : ModConfig.read(path).breakSpiderwebDelay();
    }

    // No client classes here: the same mod JAR must load on a dedicated server.
    public static void saveEditedDelay(int delay, net.minecraft.server.MinecraftServer server,
                                      java.util.function.Consumer<net.minecraft.network.chat.Component> feedback) {
        Runnable save = () -> {
            if (server != null && (!server.isSingleplayer() || !handlers.containsKey(server))) {
                feedback.accept(net.minecraft.network.chat.Component.literal(PREFIX
                        + "Config was not saved because the singleplayer world is no longer active."));
                return;
            }
            try {
                ModConfig.save(configPath(), delay);
                if (server != null) handlers.put(server, new WebBreakingHandler(delay));
                feedback.accept(net.minecraft.network.chat.Component.literal(PREFIX
                        + "Config saved. breakSpiderwebDelay: " + delay + " ms."
                        + (server == null ? " Applies when you open a singleplayer world." : " Applied to this world.")));
            } catch (java.io.IOException | RuntimeException exception) {
                LOGGER.warn(PREFIX + "Could not save config; the active configuration remains unchanged.", exception);
                feedback.accept(net.minecraft.network.chat.Component.literal(PREFIX
                        + "Could not save config. The active configuration remains unchanged. Check the config file."));
            }
        };
        if (server == null) save.run();
        else server.execute(save);
    }

    @Override
    public void onInitialize() {
        var configPath = configPath();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                ReloadCommand.register(dispatcher, configPath, (server, config) ->
                        handlers.put(server, new WebBreakingHandler(config.breakSpiderwebDelay()))));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModConfig config = ModConfig.load(configPath);
            WebBreakingHandler handler = new WebBreakingHandler(config.breakSpiderwebDelay());
            handlers.put(server, handler);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            WebBreakingHandler handler = handlers.get(server);
            if (handler != null) handler.tick(server.getPlayerList().getPlayers());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(handlers::remove);
        String version = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow()
                .getMetadata().getVersion().getFriendlyString();
        LOGGER.info(PREFIX + "Mod initialized. Version: {}", version);
    }

    private static final java.util.Map<net.minecraft.server.MinecraftServer, WebBreakingHandler> handlers
            = new java.util.IdentityHashMap<>();
}
