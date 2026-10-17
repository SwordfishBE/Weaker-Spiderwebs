package net.weaker_spiderwebs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.WebBlock;

final class WebBreakingHandler {
    private record Pending(ServerLevel level, BlockPos center, long due) {}

    private final Map<UUID, Pending> pending = new HashMap<>();
    private final int delayTicks;
    private long tick;

    WebBreakingHandler(int delayMilliseconds) {
        delayTicks = ModConfig.delayTicks(delayMilliseconds);
    }

    void tick(Iterable<ServerPlayer> players) {
        tick++;
        var online = new HashSet<UUID>();
        for (ServerPlayer player : players) {
            UUID id = player.getUUID();
            online.add(id);
            ServerLevel level = player.level();
            Pending task = pending.get(id);
            if (task != null && (player.isSpectator() || task.level() != level
                    || player.blockPosition().getX() != task.center().getX()
                    || player.blockPosition().getZ() != task.center().getZ())) {
                pending.remove(id);
                task = null;
            }
            if (task != null && tick >= task.due()) {
                breakColumn(task);
                pending.remove(id);
            }
            if (player.isSpectator() || player.tickCount % 5 != 0 || pending.containsKey(id)) continue;
            BlockPos center = BlockPos.containing(player.getX(), Math.ceil(player.getY()), player.getZ());
            if (!hasWeb(level, center)) continue;
            Pending next = new Pending(level, center, tick + delayTicks);
            if (delayTicks == 0) breakColumn(next);
            else pending.put(id, next);
        }
        pending.keySet().retainAll(online);
    }

    private static boolean hasWeb(ServerLevel level, BlockPos center) {
        return isWeb(level, center.below()) || isWeb(level, center) || isWeb(level, center.above());
    }

    private static boolean isWeb(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof WebBlock;
    }

    private static void breakColumn(Pending task) {
        for (int offset = -1; offset <= 1; offset++) {
            BlockPos pos = task.center().offset(0, offset, 0);
            if (isWeb(task.level(), pos)) task.level().destroyBlock(pos, true);
        }
    }
}
