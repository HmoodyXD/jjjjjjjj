package xyz.refinedev.practice.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.refinedev.practice.Array;
import xyz.refinedev.practice.profile.Profile;
import xyz.refinedev.practice.queue.Queue;
import xyz.refinedev.practice.util.other.DebugUtil;

@RequiredArgsConstructor
public class QueueListener implements Listener {

    private final Array plugin;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Profile profile = plugin.getProfileManager().getByPlayer(player);

        if (profile.getQueue() == null) return;
        if (!profile.isInQueue()) return;

        Queue queue = profile.getQueue();
        plugin.getQueueManager().removePlayer(queue, profile.getQueueProfile());
    }

    /**
     * Note to Source Code Viewers
     *
     * Removing this is against our TOS and you are not allowed to remove it
     * at any cost, you are also not allowed to change its colors or add your own
     * UUID to it. Don't try it otherwise it will result in a termination of your github access.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (DebugUtil.isDeveloper(player.getUniqueId())) {
            DebugUtil.sendJoinMessage(player);
        }
    }

}
