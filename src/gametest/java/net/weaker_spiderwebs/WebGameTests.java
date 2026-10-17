package net.weaker_spiderwebs;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public class WebGameTests {
    private static final BlockPos WEB = new BlockPos(2, 2, 2);

    private ServerPlayer player(GameTestHelper helper) {
        return player(helper, GameType.SURVIVAL);
    }

    private ServerPlayer player(GameTestHelper helper, GameType mode) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(mode);
        BlockPos absolute = helper.absolutePos(WEB);
        player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
        player.tickCount = 0;
        return player;
    }

    @GameTest
    public void correctDelayAndThreeBlockColumn(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (int offset = -1; offset <= 1; offset++) helper.setBlock(WEB.offset(0, offset, 0), Blocks.COBWEB);
        WebBreakingHandler handler = new WebBreakingHandler(750);
        handler.tick(List.of(player));
        for (int i = 0; i < 14; i++) handler.tick(List.of(player));
        helper.assertBlockPresent(Blocks.COBWEB, WEB);
        handler.tick(List.of(player));
        for (int offset = -1; offset <= 1; offset++) helper.assertBlockPresent(Blocks.AIR, WEB.offset(0, offset, 0));
        helper.succeed();
    }

    @GameTest
    public void zeroDelayAndUnrelatedBlocks(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.setBlock(WEB, Blocks.COBWEB);
        helper.setBlock(WEB.above(), Blocks.STONE);
        new WebBreakingHandler(0).tick(List.of(player));
        helper.assertBlockPresent(Blocks.AIR, WEB);
        helper.assertBlockPresent(Blocks.STONE, WEB.above());
        helper.succeed();
    }

    @GameTest
    public void spectatorsAndMobsDoNotTrigger(GameTestHelper helper) {
        ServerPlayer player = player(helper, GameType.SPECTATOR);
        helper.setBlock(WEB, Blocks.COBWEB);
        WebBreakingHandler handler = new WebBreakingHandler(0);
        handler.tick(List.of(player));
        handler.tick(List.of());
        helper.assertBlockPresent(Blocks.COBWEB, WEB);
        helper.succeed();
    }

    @GameTest
    public void leavingColumnCancelsCountdown(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.setBlock(WEB, Blocks.COBWEB);
        WebBreakingHandler handler = new WebBreakingHandler(500);
        handler.tick(List.of(player));
        player.setPos(player.getX() + 2, player.getY(), player.getZ());
        for (int i = 0; i < 11; i++) handler.tick(List.of(player));
        helper.assertBlockPresent(Blocks.COBWEB, WEB);
        helper.succeed();
    }

    @GameTest
    public void replacementBlockSurvives(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.setBlock(WEB, Blocks.COBWEB);
        WebBreakingHandler handler = new WebBreakingHandler(500);
        handler.tick(List.of(player));
        helper.setBlock(WEB, Blocks.STONE);
        for (int i = 0; i < 10; i++) handler.tick(List.of(player));
        helper.assertBlockPresent(Blocks.STONE, WEB);
        helper.succeed();
    }

    @GameTest
    public void disconnectClearsCountdown(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.setBlock(WEB, Blocks.COBWEB);
        WebBreakingHandler handler = new WebBreakingHandler(500);
        handler.tick(List.of(player));
        for (int i = 0; i < 12; i++) handler.tick(List.of());
        handler.tick(List.of(player));
        helper.assertBlockPresent(Blocks.COBWEB, WEB);
        helper.succeed();
    }
}
