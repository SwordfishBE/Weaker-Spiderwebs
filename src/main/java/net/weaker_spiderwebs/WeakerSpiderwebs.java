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

    @Override
    public void onInitialize() {
        var configPath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
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

    private final java.util.Map<net.minecraft.server.MinecraftServer, WebBreakingHandler> handlers
            = new java.util.IdentityHashMap<>();
}
