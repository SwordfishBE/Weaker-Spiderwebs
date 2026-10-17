# Weaker Spiderwebs

A standalone Fabric mod for Minecraft.
Cobwebs break after a configurable delay when players walk through them.

## Installation

Place `weaker_spiderwebs-<version>.jar` and Fabric API in the server's `mods` directory.
Clients do not need this mod when joining a modded server.
For singleplayer, install both mods on the client.

## Configuration

The file `config/weaker_spiderwebs.json` is created when a server or singleplayer world starts:

```json
{
  "breakSpiderwebDelay": 500
}
```

`breakSpiderwebDelay` is an integer in milliseconds, from 0 to 10000. Restart the server or reopen the singleplayer world after editing it. Existing files are never rewritten. An invalid setting produces a warning and uses the default for that session; the original file remains intact. 

## Build

Run `./gradlew build` (Windows: `.\gradlew.bat build`) with Java 25.
The installable jar is in `build/libs`; do not install the `-sources.jar`.
`build` includes configuration and text-rendering regression verification.

## License and credits

AGPL-3.0; see [LICENSE](LICENSE). Copyright (c) 2026 SwordfishBE.
