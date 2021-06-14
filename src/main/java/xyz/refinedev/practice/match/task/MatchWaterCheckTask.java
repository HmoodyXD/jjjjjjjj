package xyz.refinedev.practice.match.task;

import xyz.refinedev.practice.match.MatchState;
import xyz.refinedev.practice.profile.ProfileState;
import xyz.refinedev.practice.match.Match;
import xyz.refinedev.practice.profile.Profile;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.*;

public class MatchWaterCheckTask extends BukkitRunnable {

    private final Match match;

    public MatchWaterCheckTask(Match match) {
        this.match = match;
    }

    @Override
    public void run() {
        if (match == null || match.getAlivePlayers().isEmpty() || match.getAlivePlayers().size() <= 1) {
            return;
        }

        for (Player player : match.getAlivePlayers()) {
            if (player == null || Profile.getByUuid(player.getUniqueId()).getState() != ProfileState.IN_FIGHT) {
                return;
            }

            Block body = player.getLocation().getBlock();
            Block head = body.getRelative(BlockFace.UP);
            if (body.getType() == Material.WATER || body.getType() == Material.STATIONARY_WATER || head.getType() == Material.WATER || head.getType() == Material.STATIONARY_WATER) {
                if(match.getKit().getGameRules().isWaterKill() && match.getState() != MatchState.ENDING && !match.getKit().getGameRules().isParkour() || match.getKit().getGameRules().isSumo() && match.getState() != MatchState.ENDING) {
                    if (match.getCatcher().contains(player)) return;
                    match.handleDeath(player, null, false);
                    match.getCatcher().add(player);
                    return;
                }
                if(match.getKit().getGameRules().isParkour() || match.getKit().getGameRules().isWaterKill()) {
                    player.teleport(match.getTeamPlayer(player).getParkourCheckpoint());
                }
            }
        }
    }
}