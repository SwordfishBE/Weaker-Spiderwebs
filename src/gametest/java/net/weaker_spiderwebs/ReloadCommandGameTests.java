package net.weaker_spiderwebs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionSet;

public class ReloadCommandGameTests {
    private static final class Feedback implements CommandSource {
        final List<String> messages = new ArrayList<>();
        public void sendSystemMessage(Component message) { messages.add(message.getString()); }
        public boolean acceptsSuccess() { return true; }
        public boolean acceptsFailure() { return true; }
        public boolean shouldInformAdmins() { return false; }
    }

    @GameTest
    public void reloadPermissionsFeedbackAndFailurePreservation(GameTestHelper helper) throws Exception {
        var path = Files.createTempFile("weaker-spiderwebs-reload-", ".json");
        try {
            Files.writeString(path, "{\"breakSpiderwebDelay\":750}");
            var dispatcher = new CommandDispatcher<CommandSourceStack>();
            var active = new AtomicReference<>(new ModConfig(500));
            ReloadCommand.register(dispatcher, path, (server, config) -> active.set(config));
            Feedback feedback = new Feedback();
            var source = helper.getLevel().getServer().createCommandSourceStack().withSource(feedback);
            try {
                dispatcher.execute("weaker_spiderwebs reload", source.withPermission(PermissionSet.NO_PERMISSIONS));
                helper.fail("A non-operator could reload the configuration");
            } catch (CommandSyntaxException expected) {
                helper.assertTrue(active.get().breakSpiderwebDelay() == 500, "Denied command changed config");
            }
            int result = dispatcher.execute("weaker_spiderwebs reload", source);
            helper.assertTrue(result == 1 && active.get().breakSpiderwebDelay() == 750,
                    "Successful reload did not apply the new delay");
            helper.assertTrue(feedback.messages.getLast().equals(
                    "[Weaker Spiderwebs] Config reloaded. breakSpiderwebDelay: 750 ms."), "Wrong success feedback");
            for (String invalid : List.of("broken", "{\"breakSpiderwebDelay\":10001}")) {
                Files.writeString(path, invalid);
                result = dispatcher.execute("weaker_spiderwebs reload", source);
                helper.assertTrue(result == 0 && active.get().breakSpiderwebDelay() == 750,
                        "Failed reload replaced the active configuration");
                helper.assertTrue(Files.readString(path).equals(invalid), "Failed reload rewrote the file");
                helper.assertTrue(feedback.messages.getLast().startsWith("[Weaker Spiderwebs] Config reload failed."),
                        "Wrong failure feedback");
            }
            Files.delete(path);
            result = dispatcher.execute("weaker_spiderwebs reload", source);
            helper.assertTrue(result == 0 && active.get().breakSpiderwebDelay() == 750,
                    "Missing-file reload replaced the active configuration");
            helper.assertTrue(Files.notExists(path), "Reload unexpectedly created a default config");
            helper.assertTrue(helper.getLevel().getServer().getCommands().getDispatcher()
                    .getRoot().getChild("weaker_spiderwebs") != null, "Mod command registration is missing");
            helper.succeed();
        } finally {
            Files.deleteIfExists(path);
        }
    }
}
