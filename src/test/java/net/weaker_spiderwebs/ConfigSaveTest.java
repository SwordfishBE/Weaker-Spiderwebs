package net.weaker_spiderwebs;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ConfigSaveTest {
    @TempDir Path directory;

    @Test void editingPreservesUnknownOptionsAndRejectsInvalidValues() throws Exception {
        Path file = directory.resolve("config.json");
        Files.writeString(file, "{\"breakSpiderwebDelay\":750,\"futureOption\":{\"value\":73}}");
        ModConfig.save(file, 10000);
        assertEquals(10000, ModConfig.read(file).breakSpiderwebDelay());
        assertTrue(Files.readString(file).contains("futureOption"));
        String saved = Files.readString(file);
        assertThrows(IllegalArgumentException.class, () -> ModConfig.save(file, -1));
        assertThrows(IllegalArgumentException.class, () -> ModConfig.save(file, 10001));
        assertEquals(saved, Files.readString(file));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void malformedOrInvalidFileCannotBeOverwritten() throws Exception {
        Path file = directory.resolve("config.json");
        for (String content : new String[]{"{broken", "[]", "{\"breakSpiderwebDelay\":-1}"}) {
            Files.writeString(file, content);
            assertThrows(RuntimeException.class, () -> ModConfig.save(file, 500));
            assertEquals(content, Files.readString(file));
        }
    }

    @Test void editorCanCreateConfigWithoutOpeningAWorld() throws Exception {
        Path file = directory.resolve("new/config.json");
        ModConfig.save(file, 0);
        assertEquals(0, ModConfig.read(file).breakSpiderwebDelay());
    }
}
