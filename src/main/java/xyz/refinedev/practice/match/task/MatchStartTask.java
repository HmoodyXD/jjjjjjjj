package xyz.refinedev.practice.match.task;

import xyz.refinedev.practice.Locale;
import xyz.refinedev.practice.essentials.Essentials;
import xyz.refinedev.practice.match.Match;
import xyz.refinedev.practice.match.MatchState;
import xyz.refinedev.practice.util.chat.CC;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.refinedev.practice.util.other.PlayerUtil;

public class MatchStartTask extends BukkitRunnable {

    private final Match match;
    private int ticks;

    public MatchStartTask(Match match) {
        this.match = match;
    }

    @Override
    public void run() {
        int seconds = 5 - ticks;
        if (match.isEnding()) {
            cancel();
            return;
        }

        final String replace = Locale.MATCH_COUNTDOWN.toString().replace("<seconds>", String.valueOf((seconds - 2)));
        if (match.isHCFMatch()) {
            if (seconds == 2) {
                match.getPlayers().forEach(PlayerUtil::allowMovement);
                match.setState(MatchState.FIGHTING);
                match.setStartTimestamp(System.currentTimeMillis());
                match.broadcastMessage(Locale.MATCH_STARTED.toString());
                if (Essentials.getMeta().isDisclaimerEnabled()) {
                    match.broadcastMessage("");
                    Locale.MATCH_DISCLAIMER.toList().forEach(match::broadcastMessage);
                }
                match.broadcastSound(Sound.LEVEL_UP);
                cancel();
                return;
            }
            match.broadcastMessage(replace);
            match.broadcastSound(Sound.NOTE_PLING);
        } else {
            if (match.getKit().getGameRules().isSumo() || match.getKit().getGameRules().isParkour()) {
                if (seconds == 2) {
                    match.getPlayers().forEach(PlayerUtil::allowMovement);
                    match.setState(MatchState.FIGHTING);
                    match.setStartTimestamp(System.currentTimeMillis());
                    match.broadcastMessage(Locale.MATCH_ROUND.toString());
                    match.broadcastSound(Sound.NOTE_BASS);
                    for (Player player : match.getPlayers()) {
                        player.getInventory().remove(Material.INK_SACK);
                        player.updateInventory();
                        for (Player oPlayer : match.getPlayers()) {
                            if (player.equals(oPlayer))
                                continue;
                            player.showPlayer(oPlayer);
                            oPlayer.showPlayer(player);
                        }
                    }
                    cancel();
                    return;
                }
                match.broadcastMessage(Locale.MATCH_ROUND_COUNTDOWN.toString().replace("<seconds>", String.valueOf((seconds - 2))));
                match.broadcastSound(Sound.NOTE_PLING);
            } else {
                if (seconds == 2) {
                    match.getPlayers().forEach(PlayerUtil::allowMovement);
                    match.setState(MatchState.FIGHTING);
                    match.setStartTimestamp(System.currentTimeMillis());
                    match.broadcastMessage(Locale.MATCH_STARTED.toString());
                    if (Essentials.getMeta().isDisclaimerEnabled()) {
                        match.broadcastMessage("");
                        for ( String string : Locale.MATCH_DISCLAIMER.toList()) {
                            match.broadcastMessage(CC.translate(string));
                        }
                    }
                    match.broadcastSound(Sound.LEVEL_UP);
                    match.getPlayers().forEach(player -> {
                        player.getInventory().remove(Material.INK_SACK);
                        player.updateInventory();
                    });
                    cancel();
                    return;
                }

                match.broadcastMessage(replace);
                match.broadcastSound(Sound.NOTE_PLING);
            }
        }
        ticks++;
    }

}