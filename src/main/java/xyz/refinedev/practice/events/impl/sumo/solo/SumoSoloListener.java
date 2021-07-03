package xyz.refinedev.practice.events.impl.sumo.solo;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.refinedev.practice.Array;
import xyz.refinedev.practice.events.Event;
import xyz.refinedev.practice.events.EventState;
import xyz.refinedev.practice.profile.Profile;
import xyz.refinedev.practice.util.location.BlockUtil;
import xyz.refinedev.practice.util.other.PlayerUtil;

/**
 * This Project is property of Refine Development © 2021
 * Redistribution of this Project is not allowed
 *
 * @author Drizzy
 * Created at 6/25/2021
 * Project: Array
 */

public class SumoSoloListener implements Listener {
    
    private final Array plugin = Array.getInstance();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());
        Player player = event.getPlayer();
        if (profile.isInEvent() && profile.getEvent().isSumo()) {
            if (!profile.getEvent().isFighting(player.getUniqueId())) {
                event.setCancelled(true);
            }
        } else if (profile.getEvent() != null && profile.getEvent().getSpectators().contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());
        if (profile.isInEvent() && profile.getEvent().isSumo() || (profile.getEvent() != null && profile.getEvent().getSpectators().contains(event.getPlayer().getUniqueId()))) {
            if (profile.isInEvent() && profile.getEvent().isSumo()) {
                if (!profile.getEvent().isFighting(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            } else if (profile.getEvent() != null && profile.getEvent().getSpectators().contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onHit(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Player player = ((Player) event.getEntity()).getPlayer();
            Profile profile = Profile.getByUuid(player.getUniqueId());
            if (profile.isInEvent() && profile.getEvent().isSumo()) {
                if (!profile.getEvent().isFighting(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            } else if (profile.getEvent() != null && profile.getEvent().getSpectators().contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Profile profile = Profile.getByUuid(player.getUniqueId());
            Event sumo = profile.getEvent();
            
            if (profile.isInEvent() && profile.getEvent().isSumo()) {
                if (event.getCause() == EntityDamageEvent.DamageCause.VOID || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    event.setCancelled(true);
                    event.getEntity().setFireTicks(0);
                    
                    if (!profile.getEvent().isFighting() || !profile.getEvent().isFighting(player.getUniqueId())) {
                        player.teleport(plugin.getEventManager().getSpectator(sumo));
                        return;
                    }  else if (profile.getEvent() != null && profile.getEvent().getSpectators().contains(player.getUniqueId())) {
                        event.setCancelled(true);
                        return;
                    }
                    
                    PlayerUtil.spectator(player);
                    player.teleport(plugin.getEventManager().getSpectator(sumo));
                    profile.getEvent().handleDeath(player);
                    return;
                }

                if (profile.getEvent() != null) {
                    if (!profile.getEvent().isFighting() || !profile.getEvent().isFighting(player.getUniqueId())) {
                        event.setCancelled(true);
                        return;
                    } else if (profile.getEvent() != null && profile.getEvent().getSpectators().contains(player.getUniqueId())) {
                        event.setCancelled(true);
                    }

                    event.setDamage(0);
                    player.setHealth(20.0);
                    player.updateInventory();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            if (((Projectile) event.getDamager()).getShooter() instanceof Player) {
                attacker = (Player) ((Projectile) event.getDamager()).getShooter();
            } else {
                event.setCancelled(true);
                return;
            }
        } else {
            event.setCancelled(true);
            return;
        }

        if (attacker != null && event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            Profile damagedProfile = Profile.getByUuid(damaged.getUniqueId());
            Profile attackerProfile = Profile.getByUuid(attacker.getUniqueId());

            if (damagedProfile.isInEvent() && damagedProfile.getEvent().isSumo() && attackerProfile.isInEvent() && attackerProfile.getEvent().isSumo()) {
                Event sumo = damagedProfile.getEvent();

                if (!sumo.isFighting() || !sumo.isFighting(damaged.getUniqueId()) || !sumo.isFighting(attacker.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());

        if (profile.isInEvent() && profile.getEvent().isSumo()) {
            profile.getEvent().handleLeave(event.getPlayer());
        }
    }

    @EventHandler
    public void playerMoveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        Profile profile = Profile.getByPlayer(player);
        Event profileEvent = profile.getEvent();

       if (profile.isInEvent()) {
            if (profileEvent.getState() == EventState.ROUND_FIGHTING) {
                if (BlockUtil.isOnLiquid(to, 0) || BlockUtil.isOnLiquid(to, 1)) {
                    profileEvent.handleDeath(player);
                    ((CraftPlayer) player).getHandle().playerConnection.checkMovement=false;
                }
            }

            if (to.getX() != from.getX() || to.getZ() != from.getZ()) {
                if (profileEvent.getState() == EventState.ROUND_STARTING && profileEvent.getRoundPlayerA().getPlayer() == player || profileEvent.getState() == EventState.ROUND_STARTING && profileEvent.getRoundPlayerB().getPlayer() == player) {
                    player.teleport(from);
                    ((CraftPlayer) player).getHandle().playerConnection.checkMovement=false;
                }
            }
        }

    }
}