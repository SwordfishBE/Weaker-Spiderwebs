package net.weaker_spiderwebs;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ModConfigTest {
    @TempDir Path directory;

    @Test void createsDefaultOnlyWhenMissing() throws Exception {
        Path path = directory.resolve("config.json");
        assertEquals(500, ModConfig.load(path).breakSpiderwebDelay());
        assertTrue(Files.readString(path).contains("500"));
    }

    @Test void preservesExistingSettingsAndUnknownFields() throws Exception {
        Path path = directory.resolve("config.json");
        String content = "{\"breakSpiderwebDelay\":750,\"custom\":true}";
        Files.writeString(path, content);
        assertEquals(750, ModConfig.load(path).breakSpiderwebDelay());
        assertEquals(content, Files.readString(path));
    }

    @Test void preservesInvalidFilesAndFallsBack() throws Exception {
        Path path = directory.resolve("config.json");
        for (String content : new String[]{"broken", "null", "[]", "{\"breakSpiderwebDelay\":null}",
                "{\"breakSpiderwebDelay\":-1}", "{\"breakSpiderwebDelay\":10001}",
                "{\"breakSpiderwebDelay\":0.5}", "{\"breakSpiderwebDelay\":\"500\"}"}) {
            Files.writeString(path, content);
            assertEquals(500, ModConfig.load(path).breakSpiderwebDelay(), content);
            assertEquals(content, Files.readString(path));
        }
    }

    @Test void supportsBothLimitsAndMissingOption() throws Exception {
        Path path = directory.resolve("config.json");
        for (int delay : new int[]{0, 10000}) {
            Files.writeString(path, "{\"breakSpiderwebDelay\":" + delay + "}");
            assertEquals(delay, ModConfig.load(path).breakSpiderwebDelay());
        }
        Files.writeString(path, "{}");
        assertEquals(500, ModConfig.load(path).breakSpiderwebDelay());
        assertEquals("{}", Files.readString(path));
    }

    @Test void roundsDelayUpToFullTicksWithoutLosingSubseconds() {
        assertEquals(0, ModConfig.delayTicks(0));
        assertEquals(1, ModConfig.delayTicks(1));
        assertEquals(1, ModConfig.delayTicks(50));
        assertEquals(2, ModConfig.delayTicks(51));
        assertEquals(10, ModConfig.delayTicks(500));
        assertEquals(15, ModConfig.delayTicks(750));
        assertEquals(200, ModConfig.delayTicks(10000));
    }
}
