package xyz.refinedev.practice.match;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import xyz.refinedev.practice.Array;
import xyz.refinedev.practice.Locale;
import xyz.refinedev.practice.api.events.match.MatchPlayerSetupEvent;
import xyz.refinedev.practice.arena.Arena;
import xyz.refinedev.practice.arena.ArenaType;
import xyz.refinedev.practice.arena.impl.TheBridgeArena;
import xyz.refinedev.practice.kit.KitInventory;
import xyz.refinedev.practice.match.team.Team;
import xyz.refinedev.practice.match.team.TeamPlayer;
import xyz.refinedev.practice.match.types.kit.SoloBridgeMatch;
import xyz.refinedev.practice.match.types.kit.TeamBridgeMatch;
import xyz.refinedev.practice.profile.Profile;
import xyz.refinedev.practice.profile.hotbar.HotbarType;
import xyz.refinedev.practice.util.chat.CC;
import xyz.refinedev.practice.util.inventory.ItemBuilder;
import xyz.refinedev.practice.util.location.Cuboid;
import xyz.refinedev.practice.util.other.Cooldown;
import xyz.refinedev.practice.util.other.PlayerUtil;
import xyz.refinedev.practice.util.other.TaskUtil;
import xyz.refinedev.practice.util.other.TimeUtil;

import java.util.ArrayList;
import java.util.List;

public class MatchListener implements Listener {

    public Array plugin = Array.getInstance();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Profile profile = Profile.getByUuid(player.getUniqueId());
        if (profile.isInFight()) {
            Match match = profile.getMatch();
            if (!profile.getMatch().isHCFMatch()) {
                if (match.getKit().getGameRules().isBuild() && profile.getMatch().isFighting()) {
                    if (match.getKit().getGameRules().isSpleef()) {
                        event.setCancelled(true);
                        return;
                    }

                    Block block = event.getBlockPlaced();

                    Arena arena = match.getArena();
                    int x = (int) block.getLocation().getX();
                    int y = (int) block.getLocation().getY();
                    int z = (int) block.getLocation().getZ();

                    if (y > arena.getMaxBuildHeight()) {
                        player.sendMessage(Locale.MATCH_MAX_BUILD.toString());
                        event.setCancelled(true);
                        return;
                    }

                    if (arena instanceof TheBridgeArena) {
                        TheBridgeArena standaloneArena = (TheBridgeArena) arena;
                        if (standaloneArena.getBlueCuboid() != null && standaloneArena.getBlueCuboid().contains(block)) {
                            player.sendMessage(Locale.MATCH_BRIDGE_BLOCK.toString());
                            event.setCancelled(true);
                            return;
                        }
                        if (standaloneArena.getRedCuboid() != null && standaloneArena.getRedCuboid().contains(block)) {
                            player.sendMessage(Locale.MATCH_BRIDGE_BLOCK.toString());
                            event.setCancelled(true);
                            return;
                        }
                    }

                    Cuboid cuboid = new Cuboid(arena.getMax(), arena.getMin());

                    if (x >= cuboid.getX1() && x <= cuboid.getX2() && y >= cuboid.getY1() && y <= cuboid.getY2() && z >= cuboid.getZ1() && z <= cuboid.getZ2()) {
                        match.getPlacedBlocks().add(block.getLocation());
                        Location down = block.getLocation().subtract(0, 1, 0);
                        if (down.getBlock().getType() == Material.GRASS){
                            match.getChangedBlocks().add(down.getBlock().getState());
                        }
                    } else {
                        player.sendMessage(Locale.MATCH_BUILD_OUTSIDE.toString());
                        event.setCancelled(true);
                    }
                } else {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Profile profile = Profile.getByUuid(player.getUniqueId());
        Match match = profile.getMatch();

        if (!profile.isInFight()) return;
        if (player.getLocation().getBlock().getType() != Material.ENDER_PORTAL && player.getLocation().getBlock().getType() != Material.ENDER_PORTAL_FRAME)
            return;

        if (match instanceof SoloBridgeMatch) {
            SoloBridgeMatch soloBridgeMatch=(SoloBridgeMatch) match;
            soloBridgeMatch.onPortalEnter(player);
        } else if (match instanceof TeamBridgeMatch) {
            TeamBridgeMatch teamBridgeMatch=(TeamBridgeMatch) match;
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockFromEvent(BlockFromToEvent event) {
        Arena foundArena = null;
        int x = event.getBlock().getX();
        int y = event.getBlock().getY();
        int z = event.getBlock().getZ();

        for (Arena arena : Arena.getArenas()) {
            if (!(arena.getType() == ArenaType.STANDALONE || arena.getType() == ArenaType.DUPLICATE)) {
                continue;
            }
            if (!arena.isActive()) {
                continue;
            }

            Cuboid cuboid = new Cuboid(arena.getMax(), arena.getMin());
            if (x >= cuboid.getX1() && x <= cuboid.getX2() && y >= cuboid.getY1() && y <= cuboid.getY2() && z >= cuboid.getZ1() && z <= cuboid.getZ2()) {
                foundArena = arena;
                break;
            }
        }

        if (foundArena == null) {
            return;
        }

        for (Match match : Match.getMatches()) {
            if (match.getArena().equals(foundArena)) {
                if (match.isFighting()) {
                    match.getPlacedBlocks().add(event.getToBlock().getLocation());
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreakEvent(BlockBreakEvent event) {
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());

        if (profile.isInFight()) {
            Match match = profile.getMatch();
            if (!profile.getMatch().isHCFMatch()) {
                if (match.getKit().getGameRules().isBuild() && profile.getMatch().isFighting()) {
                    if (match.getKit().getGameRules().isSpleef()) {
                        if (event.getBlock().getType() == Material.SNOW_BLOCK || event.getBlock().getType() == Material.SNOW) {
                            match.getChangedBlocks().add(event.getBlock().getState());
                            event.getBlock().setType(Material.AIR);
                            event.getPlayer().getInventory().addItem(new ItemStack(Material.SNOW_BALL, 4));
                            event.getPlayer().updateInventory();
                        } else {
                            event.setCancelled(true);
                        }
                    } else if (match.getKit().getGameRules().isBoxuhc()) {
                        if (event.getBlock().getType() == Material.WOOD) {
                            match.getChangedBlocks().add(event.getBlock().getState());
                            event.getBlock().setType(Material.AIR);
                            event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemBuilder(Material.WOOD).build());
                            event.getPlayer().updateInventory();
                        } else {
                            event.setCancelled(true);
                        }
                    } else if (!match.getPlacedBlocks().remove(event.getBlock().getLocation())) {
                        event.setCancelled(true);
                    }
                } else {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmptyEvent(PlayerBucketEmptyEvent event) {
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());
        if (profile.isInFight()) {
            Match match = profile.getMatch();
            if (!profile.getMatch().isHCFMatch()) {
                if (match.getKit().getGameRules().isBuild() && profile.getMatch().isFighting()) {
                    Arena arena = match.getArena();
                    Block block = event.getBlockClicked().getRelative(event.getBlockFace());

                    int x = (int) block.getLocation().getX();
                    int z = (int) block.getLocation().getZ();
                    int y = (int) block.getLocation().getY();

                    if (y > arena.getMaxBuildHeight()) {
                        event.getPlayer().sendMessage(Locale.MATCH_MAX_BUILD.toString());
                        event.setCancelled(true);
                        return;
                    }

                    Cuboid cuboid = new Cuboid(arena.getMax(), arena.getMin());

                    if (x >= cuboid.getX1() && x <= cuboid.getX2() && y >= cuboid.getY1() && y <= cuboid.getY2() && z >= cuboid.getZ1() && z <= cuboid.getZ2()) {
                        match.getPlacedBlocks().add(block.getLocation());
                    } else {
                        event.setCancelled(true);
                    }
                } else {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPickupItemEvent(PlayerPickupItemEvent event) {
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());
        if (profile.isSpectating()) {
            event.setCancelled(true);
            return;
        }
        if (profile.isInFight()) {
            Match match = profile.getMatch();
            if (!profile.getMatch().getTeamPlayer(event.getPlayer()).isAlive()) {
                event.setCancelled(true);
                return;
            }

            if (match.getState() == MatchState.ENDING || match.canEnd()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());

        ItemStack itemStack = event.getItemDrop().getItemStack();
        Material itemType = itemStack.getType();
        String itemTypeName = itemType.name().toLowerCase();
        int heldSlot = player.getInventory().getHeldItemSlot();
        
        if (profile.isSpectating() || event.getItemDrop().getItemStack().getType() == Material.BOOK || event.getItemDrop().getItemStack().getType() == Material.ENCHANTED_BOOK) {
            event.setCancelled(true);
        }

        if (profile.isInFight()) {
            profile.getMatch().getEntities().add(event.getItemDrop());
        }

        if (profile.getSettings().isPreventSword()) {
            if (!PlayerUtil.hasOtherInventoryOpen(player) && heldSlot == 0 && (itemTypeName.contains("sword") || itemTypeName.contains("axe") || itemType == Material.BOW)) {
                player.sendMessage(Locale.MATCH_SWORD_DROP.toString());
                event.setCancelled(true);
            }
        }

        if (itemType == Material.GLASS_BOTTLE || itemType == Material.BOWL || itemType == Material.INK_SACK) {
            event.getItemDrop().remove();
        }
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        event.setDeathMessage(null);

        Player player = event.getEntity().getPlayer();
        Profile profile = Profile.getByPlayer(player);
        Match match = profile.getMatch();
        player.spigot().respawn();

        if (profile.isInFight()) {
            List<Item> entities = new ArrayList<>();
            event.getDrops().forEach(itemStack -> {
                if (itemStack.getType() != Material.BOOK && itemStack.getType() != Material.ENCHANTED_BOOK && itemStack.getType() != Material.BLAZE_POWDER && itemStack.getType() != Material.GOLD_BARDING && itemStack.getType() != Material.DIAMOND_BARDING) {
                    entities.add(event.getEntity().getLocation().getWorld().dropItemNaturally(event.getEntity().getLocation(), itemStack));
                }
            });
            event.getDrops().clear();
            match.getEntities().addAll(entities);
            match.handleDeath(player);
        }
    }

    /**
     * Apply the below health display for
     * Build/Show health kit matches
     *
     * @param event {@link MatchPlayerSetupEvent}
     */
    @EventHandler
    public void onStart(MatchPlayerSetupEvent event) {
        if (event.getMatch() == null || event.getMatch().getKit() == null) return;
        if (!event.getMatch().getKit().getGameRules().isBuild() && !event.getMatch().getKit().getGameRules().isShowHealth()) return;
        for ( TeamPlayer otherPlayerTeam : event.getMatch().getTeamPlayers() ) {
            Player otherPlayer = otherPlayerTeam.getPlayer();
            Scoreboard scoreboard = event.getPlayer().getScoreboard();
            Objective objective = scoreboard.getObjective(DisplaySlot.BELOW_NAME);

            if (objective == null) {
                objective = scoreboard.registerNewObjective("showhealth", "health");
            }

            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            objective.setDisplayName(ChatColor.RED + org.apache.commons.lang.StringEscapeUtils.unescapeJava("\u2764"));
            objective.getScore(otherPlayer.getName()).setScore((int) Math.floor(otherPlayer.getHealth() / 2));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(event.getPlayer().getLocation());
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());
        if (profile.isInFight()) {
            profile.getMatch().onRespawn(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunchEvent(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof ThrownPotion && event.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            Profile shooterProfile = Profile.getByUuid(shooter.getUniqueId());

            if (shooterProfile.isInFight() && shooterProfile.getMatch().isFighting()) {
                shooterProfile.getMatch().getTeamPlayer(shooter).incrementPotionsThrown();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow && event.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            Profile shooterProfile = Profile.getByUuid(shooter.getUniqueId());

            if (shooterProfile.isInFight()) {
                shooterProfile.getMatch().getEntities().add(event.getEntity());
                shooterProfile.getMatch().getTeamPlayer(shooter).handleHit();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPotionSplashEvent(PotionSplashEvent event) {
        if (event.getPotion().getShooter() instanceof Player) {
            Player shooter = (Player) event.getPotion().getShooter();
            Profile shooterProfile = Profile.getByUuid(shooter.getUniqueId());

            if (shooterProfile.isSpectating()) {
                event.setCancelled(true);
            }

            if (shooterProfile.isInFight() && event.getIntensity(shooter) <= 0.5) {
                shooterProfile.getMatch().getTeamPlayer(shooter).incrementPotionsMissed();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            Profile profile = Profile.getByUuid(event.getEntity().getUniqueId());

            if (profile.isInFight() && !profile.getMatch().isHCFMatch()  && !profile.getMatch().getKit().getGameRules().isRegen()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Profile profile = Profile.getByUuid(player.getUniqueId());
            if (profile.isInFight()) {
                Match match = profile.getMatch();
                if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    event.setDamage(1000.0);
                    return;
                }
                if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                    if (match.isTheBridgeMatch() ||match.getKit() != null && match.getKit().getGameRules().isDisableFallDamage()) {
                        event.setCancelled(true);
                    }
                }
                if (event.getCause() == EntityDamageEvent.DamageCause.LAVA && profile.getMatch().getKit().getGameRules().isLavaKill()) {
                    profile.getMatch().handleDeath(player, null, false);
                    return;
                }
                if (profile.getMatch().isEnding()) {
                    event.setCancelled(true);
                    return;
                }
                if (profile.getMatch().getKit().getGameRules().isParkour()) {
                    event.setCancelled(true);
                    return;
                }
                if (!profile.getMatch().isFighting()) {
                    event.setCancelled(true);
                    return;
                }
                if (profile.getMatch().getKit().getGameRules().isSumo()) {
                    event.setDamage(0.0);
                    player.setHealth(20.0);
                    player.updateInventory();
                }
            }
        }
    }

    @EventHandler
    public void onVertMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Profile profile = Profile.getByUuid(player.getUniqueId());
        Match match = profile.getMatch();
        if (!profile.isInMatch()) return;
        if (match.isEnding()) return;

        if (event.getFrom().getBlockY() - 1 > event.getTo().getBlockY() && match.getArena().getFallDeathHeight() >= event.getTo().getBlockY()) {
            if (match.getKit().getGameRules().isVoidSpawn()) {
                TeamPlayer teamPlayer = match.getTeamPlayer(player);
                if (teamPlayer != null) {
                    player.teleport(teamPlayer.getPlayerSpawn());
                } else {
                    player.teleport(match.getMidSpawn());
                }
                return;
            }
            match.handleDeath(event.getPlayer());
            if (match.isTheBridgeMatch()) {
                match.handleRespawn(event.getPlayer());
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else {
            if (!(event.getDamager() instanceof Projectile)) {
                event.setCancelled(true);
                return;
            }
            if (!(((Projectile) event.getDamager()).getShooter() instanceof Player)) {
                event.setCancelled(true);
                return;
            }
            attacker = (Player) ((Projectile) event.getDamager()).getShooter();
        }

        if (attacker == null) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player damaged = (Player) event.getEntity();
        Profile damagedProfile = Profile.getByUuid(damaged.getUniqueId());
        Profile attackerProfile = Profile.getByUuid(attacker.getUniqueId());
        if (attackerProfile.isSpectating() || damagedProfile.isSpectating()) {
            event.setCancelled(true);
            return;
        }

        if (!damagedProfile.isInFight()) return;
        if (!attackerProfile.isInFight()) return;

        Match match = attackerProfile.getMatch();
        if (attackerProfile.getMatch().getKit().getGameRules().isSpleef() && !(event.getDamager() instanceof Projectile)) {
            event.setCancelled(true);
        }
        if (damagedProfile.getMatch().getKit().getGameRules().isSpleef() && !(event.getDamager() instanceof Projectile)) {
            event.setCancelled(true);
            return;
        }
        if (!damagedProfile.getMatch().getMatchId().equals(attackerProfile.getMatch().getMatchId())) {
            event.setCancelled(true);
            return;
        }
        if (damagedProfile.getMatch().getState().equals(MatchState.STARTING)) {
            event.setCancelled(true);
            return;
        }
        if (!match.getTeamPlayer(damaged).isAlive() || !match.getTeamPlayer(attacker).isAlive() && !match.isFreeForAllMatch()) {
            event.setCancelled(true);
            return;
        }

        if (match.isSoloMatch() || match.isFreeForAllMatch()) {
            attackerProfile.getMatch().getTeamPlayer(attacker).handleHit();
            damagedProfile.getMatch().getTeamPlayer(damaged).resetCombo();
            if (!(event.getDamager() instanceof Arrow)) return;
            double health = Math.ceil(damaged.getHealth() - event.getFinalDamage()) / 2.0;
            attacker.sendMessage(Locale.MATCH_BOW_HIT.toString()
                    .replace("<damaged_name>", damaged.getName())
                    .replace("<damaged_health>", String.valueOf(health)));
            return;
        }

        if (!match.isTeamMatch() || !match.isHCFMatch()) return;

        Team attackerTeam = match.getTeam(attacker);
        Team damagedTeam = match.getTeam(damaged);

        if (attackerTeam == null || damagedTeam == null) {
            event.setCancelled(true);
            return;
        }
        if (attackerTeam.equals(damagedTeam)) {
            event.setCancelled(true);
            return;
        }

        attackerProfile.getMatch().getTeamPlayer(attacker).handleHit();
        damagedProfile.getMatch().getTeamPlayer(damaged).resetCombo();

        if (!(event.getDamager() instanceof Arrow)) return;

        double health = Math.ceil(damaged.getHealth() - event.getFinalDamage()) / 2.0;

        if (match.getKit() == null || match.getKit().getGameRules().isBowHP()) {
            if (!attacker.getName().equalsIgnoreCase(damaged.getName())) {
                attacker.sendMessage(Locale.MATCH_BOW_HIT.toString()
                        .replace("<damaged_name>", damaged.getName())
                        .replace("<damaged_health>", String.valueOf(health)));
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsumeEvent(PlayerItemConsumeEvent event) {
        if (!event.getItem().getType().equals(Material.POTION)) return;
        if (!event.getItem().getType().equals(Material.GLASS_BOTTLE)) return;
        if (!plugin.getConfigHandler().isREMOVED_BOTTLES()) return;

        TaskUtil.runLaterAsync(() -> event.getPlayer().setItemInHand(new ItemStack(Material.AIR)), 1L);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Profile profile = Profile.getByUuid(player.getUniqueId());
            if (profile.isInFight() && profile.getMatch() != null && profile.getMatch().getKit().getGameRules().isAntiFoodLoss()) {
                event.setFoodLevel(20);
            }
            if (profile.isInFight() && profile.getMatch().isFighting()) {
                if (event.getFoodLevel() >= 20) {
                    event.setFoodLevel(20);
                    player.setSaturation(20.0f);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Profile profile = Profile.getByPlayer(player);
        Match match = profile.getMatch();

        if (profile.isInFight()) {
            if (match.isStarting()) {
                match.getTask().cancel();
            }
            profile.getMatch().handleDeath(event.getPlayer(), null, true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Profile profile = Profile.getByUuid(event.getWhoClicked().getUniqueId());
        if (profile.isSpectating()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryInteract(InventoryInteractEvent event) {
        Profile profile = Profile.getByUuid(event.getWhoClicked().getUniqueId());
        if (profile.isSpectating()) {
            event.setCancelled(true);
        }
    }

    /**
     * The main code for CPS Counter
     * Fully coded by Veltus @ PurgeCommunity
     *
     * @param event {@link PlayerInteractEvent}
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getAction().name().contains("LEFT")) {
            Profile profile = Profile.getByPlayer(event.getPlayer());

            if (profile.isInFight()) {
                try {
                    TeamPlayer player=profile.getMatch().getTeamPlayer(event.getPlayer());

                    List<Long> list;

                    if (player.getCpsMap().containsKey(player.getUuid())) {
                        list = player.getCpsMap().get(player.getUuid());
                    } else {
                        list = new ArrayList<>();
                    }
                    list.add(System.currentTimeMillis());

                    player.getCpsMap().put(event.getPlayer().getUniqueId(), list);
                } catch (Exception ignored) {}
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile instanceof EnderPearl) {
            EnderPearl enderPearl = (EnderPearl) projectile;
            ProjectileSource source = enderPearl.getShooter();

            if (source instanceof Player) {
                Player shooter = (Player) source;
                Profile profile = Profile.getByUuid(shooter.getUniqueId());

                if (profile.isInFight()) {
                    if (!profile.getEnderpearlCooldown().hasExpired()) {
                        String time = TimeUtil.millisToSeconds(profile.getEnderpearlCooldown().getRemaining());
                        String context = "second" + (time.equalsIgnoreCase("1.0") ? "" : "s");

                        shooter.sendMessage(Locale.MATCH_PEARL_COOLDOWN.toString().replace("<cooldown>", time + " " + context));
                        shooter.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));

                        event.setCancelled(true);
                    } else {
                        profile.setEnderpearlCooldown(new Cooldown(16000L));
                        profile.getMatch().onPearl(shooter, enderPearl);
                    }
                }
            }
        } else if (projectile instanceof Arrow) {
            Arrow enderPearl = (Arrow) projectile;
            ProjectileSource source = enderPearl.getShooter();

            if (source instanceof Player) {
                Player shooter = (Player) source;
                Profile profile = Profile.getByPlayer(shooter);

                if (profile.isInFight() && profile.getMatch().isTheBridgeMatch()) {
                    if (!profile.getBowCooldown().hasExpired()) {
                        String time = TimeUtil.millisToSeconds(profile.getBowCooldown().getRemaining());
                        String context = "second" + (time.equalsIgnoreCase("1.0") ? "" : "s");

                        shooter.sendMessage(Locale.MATCH_BOW_COOLDOWN.toString().replace("<cooldown>", time + " " + context));

                        event.setCancelled(true);
                    } else {
                        profile.setBowCooldown(new Cooldown(TimeUtil.parseTime(plugin.getConfigHandler().getBOW_COOLDOWN() + "s")));
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleportPearl(PlayerTeleportEvent event) {
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && profile.isInFight()) {
            profile.getMatch().removePearl(event.getPlayer(), false);
            Location target = event.getTo();
            target.setX(target.getBlockX() + 0.5);
            target.setZ(target.getBlockZ() + 0.5);
            event.setTo(target);
        }
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEvent(PlayerInteractEvent event) {
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());
        if (profile.isSpectating()) {
            event.setCancelled(true);
        }
        if (profile.isInFight()) {
            if (event.getItem() != null && event.getAction().name().contains("RIGHT")) {

            if (event.getItem().hasItemMeta() && event.getItem().getItemMeta().hasDisplayName()) {
                if (event.getItem().isSimilar(plugin.getHotbarManager().getHotbarItem(HotbarType.DEFAULT_KIT).getItem())) {
                    KitInventory kitLoadout = profile.getMatch().getKit().getKitInventory();
                    event.getPlayer().getInventory().setArmorContents(kitLoadout.getArmor());
                    event.getPlayer().getInventory().setContents(kitLoadout.getContents());
                    event.getPlayer().updateInventory();
                    event.setCancelled(true);
                    return;
                }
            }
                if (!profile.getMatch().isHCFMatch() && event.getItem().hasItemMeta() && event.getItem().getItemMeta().hasDisplayName() && event.getItem().getItemMeta().hasLore()) {
                    String displayName=CC.translate(event.getItem().getItemMeta().getDisplayName());
                    if (displayName.endsWith(" (Right-Click)")) {
                        String kitName=displayName.replace(" (Right-Click)", "");
                        for ( KitInventory kitInventory2 : profile.getStatisticsData().get(profile.getMatch().getKit()).getLoadouts() ) {
                            if (kitInventory2 != null && ChatColor.stripColor(kitInventory2.getCustomName()).equals(ChatColor.stripColor(kitName))) {
                                event.getPlayer().getInventory().setArmorContents(kitInventory2.getArmor());
                                event.getPlayer().getInventory().setContents(kitInventory2.getContents());
                                event.getPlayer().getActivePotionEffects().clear();
                                event.getPlayer().addPotionEffects(profile.getMatch().getKit().getKitInventory().getEffects());
                                event.getPlayer().updateInventory();
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                }

                Player player = event.getPlayer();
                if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) && player.getItemInHand().getType() == Material.MUSHROOM_SOUP) {
                    int health=(int) player.getHealth();
                    if (health == 20) {
                        player.getItemInHand().setType(Material.MUSHROOM_SOUP);
                    } else if (health >= 13) {
                        player.setHealth(20.0);
                        player.getItemInHand().setType(Material.BOWL);
                    } else {
                        player.setHealth(health + 7);
                        player.getItemInHand().setType(Material.BOWL);
                    }
                }
                if ((event.getItem().getType() == Material.ENDER_PEARL || (event.getItem().getType() == Material.POTION && event.getItem().getDurability() >= 16000)) && profile.isInFight() && profile.getMatch().isStarting()) {
                    event.setCancelled(true);
                    player.updateInventory();
                    return;
                }
                if (event.getItem().getType() == Material.ENDER_PEARL && event.getClickedBlock() == null) {
                    if (!profile.isInFight() || (profile.isInFight() && !profile.getMatch().isFighting())) {
                        event.setCancelled(true);
                        return;
                    }
                    if (profile.getMatch().isStarting()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPressurePlate(PlayerInteractEvent event) {
        Profile profile = Profile.getByUuid(event.getPlayer().getUniqueId());
        Match match = profile.getMatch();
        if (profile.isInFight() && match.isSoloMatch() && match.getKit().getGameRules().isParkour()) {
            if (event.getAction().equals(Action.PHYSICAL) && event.getClickedBlock().getType() == Material.GOLD_PLATE && profile.getPlates() != null) {
                if (profile.getPlates().contains(event.getClickedBlock().getLocation())) return;
                profile.getPlates().add(event.getClickedBlock().getLocation());
                if (match.getOpponentPlayer(event.getPlayer()) != null) {
                    match.handleDeath(event.getPlayer(), null, false);
                } else {
                    match.end();
                }
            }
            if (event.getAction().equals(Action.PHYSICAL) && event.getClickedBlock().getType() == Material.IRON_PLATE && profile.getPlates() != null) {
                if (profile.getPlates().contains(event.getClickedBlock().getLocation())) return;
                match.getTeamPlayer(event.getPlayer()).setParkourCheckpoint(event.getPlayer().getLocation());
                event.getPlayer().sendMessage(Locale.MATCH_CHECKPOINT.toString());
                profile.getPlates().add(event.getClickedBlock().getLocation());
            }
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
      for( LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player) {
                Player player = (Player) entity;
                Profile profile = Profile.getByUuid(player.getUniqueId());
                if (!profile.isSpectating()) {
                    continue;
                }
                event.setIntensity(player, 0.0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeave(PlayerQuitEvent e) {
        Profile profile = Profile.getByUuid(e.getPlayer().getUniqueId());
        if (profile.isInFight()) {
            profile.getMatch().handleDeath(e.getPlayer(), null, true);
        } else if (profile.isInMatch()) {
            profile.getMatch().handleDeath(e.getPlayer(), null, true);
        }
        if (profile.isSpectating()) {
            profile.getMatch().removeSpectator(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamageEntity(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player damaged = (Player) e.getEntity();
            Profile damagedProfile = Profile.getByUuid(damaged.getUniqueId());
            if (damagedProfile.getMatch() != null) {
                Match match = damagedProfile.getMatch();
                if (match.isEnding()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onPearlThrow(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile instanceof EnderPearl) {
            EnderPearl enderPearl = (EnderPearl) projectile;
            ProjectileSource source = enderPearl.getShooter();

            if (source instanceof Player) {
                Player shooter = (Player) source;
                Profile profile = Profile.getByPlayer(shooter);

                if (profile.getMatch() != null && profile.getMatch().getArena() != null) {
                    if (profile.getMatch().getArena().isDisablePearls()) {
                        shooter.sendMessage(Locale.ERROR_PEARLSDISABLED.toString());
                        shooter.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
