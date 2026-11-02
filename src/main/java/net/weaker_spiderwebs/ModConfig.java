package net.weaker_spiderwebs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

record ModConfig(int breakSpiderwebDelay) {
    static final int DEFAULT_DELAY = 500;

    static synchronized ModConfig load(Path path) {
        if (Files.notExists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, "{\n  \"breakSpiderwebDelay\": 500\n}\n", StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE_NEW);
            } catch (IOException exception) {
                WeakerSpiderwebs.LOGGER.warn(WeakerSpiderwebs.PREFIX
                        + "Could not create config {}; using defaults.", path, exception);
            }
            return new ModConfig(DEFAULT_DELAY);
        }
        try {
            return read(path);
        } catch (IOException | RuntimeException exception) {
            WeakerSpiderwebs.LOGGER.warn(WeakerSpiderwebs.PREFIX
                    + "Could not read config {}; using {} ms. The file has been preserved.",
                    path, DEFAULT_DELAY, exception);
            return new ModConfig(DEFAULT_DELAY);
        }
    }

    // Reload must report errors instead of replacing an active config with defaults.
    static synchronized ModConfig read(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            JsonElement value = object.get("breakSpiderwebDelay");
            if (value == null) return new ModConfig(DEFAULT_DELAY);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("breakSpiderwebDelay must be an integer");
            }
            int delay = value.getAsBigDecimal().intValueExact();
            if (delay < 0 || delay > 10000) {
                throw new IllegalArgumentException("breakSpiderwebDelay must be between 0 and 10000");
            }
            return new ModConfig(delay);
        }
    }

    static synchronized void save(Path path, int delay) throws IOException {
        if (delay < 0 || delay > 10000) throw new IllegalArgumentException("Delay must be between 0 and 10000 ms.");
        JsonObject object = new JsonObject();
        if (Files.exists(path)) {
            // Validate the existing document before editing; keep unrecognized fields.
            read(path);
            object = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        object.addProperty("breakSpiderwebDelay", delay);
        Path directory = path.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "weaker_spiderwebs-", ".tmp");
        try {
            Files.writeString(temporary, new com.google.gson.GsonBuilder().setPrettyPrinting().create()
                    .toJson(object) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static int delayTicks(int milliseconds) {
        return (milliseconds + 49) / 50;
    }
}
