package xyz.refinedev.practice.match.menu;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import xyz.refinedev.practice.match.MatchSnapshot;
import xyz.refinedev.practice.match.team.TeamPlayer;
import xyz.refinedev.practice.util.chat.CC;
import xyz.refinedev.practice.util.inventory.InventoryUtil;
import xyz.refinedev.practice.util.inventory.ItemBuilder;
import xyz.refinedev.practice.util.menu.Button;
import xyz.refinedev.practice.util.menu.Menu;
import xyz.refinedev.practice.util.menu.button.DisplayButton;
import xyz.refinedev.practice.util.other.PotionUtil;
import xyz.refinedev.practice.util.other.TimeUtil;

import java.util.*;

@AllArgsConstructor
public class MatchDetailsMenu extends Menu {

    private final MatchSnapshot snapshot;
    private final MatchSnapshot opponent;

    @Override
    public String getTitle(Player player) {
        return "&7&lInventory of &c" + snapshot.getTeamPlayer().getUsername();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons=new HashMap<>();
        ItemStack[] fixedContents=InventoryUtil.fixInventoryOrder(snapshot.getContents());

        for ( int i=0; i < fixedContents.length; i++ ) {
            ItemStack itemStack=fixedContents[i];

            if (itemStack != null && itemStack.getType() != Material.AIR) {
                buttons.put(i, new DisplayButton(itemStack, true));
            }
        }

        for ( int i=0; i < snapshot.getArmor().length; i++ ) {
            ItemStack itemStack=snapshot.getArmor()[i];

            if (itemStack != null && itemStack.getType() != Material.AIR) {
                buttons.put(39 - i, new DisplayButton(itemStack, true));
            }
        }


        buttons.put(48, new HealthButton(snapshot.getHealth()));
        buttons.put(49, new HungerButton(snapshot.getHunger()));
        buttons.put(50, new EffectsButton(snapshot.getEffects()));

        if (snapshot.shouldDisplayRemainingPotions()) {
            buttons.put(51, new PotionsButton(snapshot.getTeamPlayer().getUsername(), snapshot.getRemainingPotions()));
        }

        buttons.put(47, new StatisticsButton(snapshot.getTeamPlayer()));

        if (this.snapshot.getSwitchTo() != null || this.opponent != null) {
            buttons.put(45, new SwitchInventoryButton(this.snapshot.getSwitchTo()));
            buttons.put(53, new SwitchInventoryButton(this.snapshot.getSwitchTo()));
        }

        return buttons;
    }

    @AllArgsConstructor
    private class SwitchInventoryButton extends Button {

        private final TeamPlayer switchTo;

        @Override
        public ItemStack getButtonItem(Player player) {
            if (opponent != null) {
                return new ItemBuilder(Material.LEVER)
                        .name("&c&lNext Inventory")
                        .lore("&7Switch to &c" + opponent.getTeamPlayer().getUsername() + "&7's inventory")
                        .build();
            }

            return new ItemBuilder(Material.LEVER)
                    .name("&c&lNext Inventory")
                    .lore("&7Switch to &c" + switchTo.getUsername() + "&7's inventory")
                    .build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            if (opponent != null) {
                new MatchDetailsMenu(opponent, snapshot).openMenu(player);
            } else {
                MatchSnapshot cachedInventory;

                try {
                    cachedInventory = MatchSnapshot.getByUuid(switchTo.getUniqueId());
                } catch (Exception e) {
                    cachedInventory = MatchSnapshot.getByName(switchTo.getUniqueId().toString());
                }

                if (cachedInventory == null) {
                    player.sendMessage(CC.RED + "Couldn't find an inventory for that ID.");
                    return;
                }

                new MatchDetailsMenu(cachedInventory, null).openMenu(player);
            }
        }

    }

    @AllArgsConstructor
    private static class HealthButton extends Button {

        private final int health;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.MELON)
                    .name("&cHealth: &7" + health + "/10&c" + StringEscapeUtils.unescapeJava("\u2764"))
                    .amount(health == 0 ? 1 : health)
                    .build();
        }

    }

    @AllArgsConstructor
    private static class HungerButton extends Button {

        private final int hunger;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.COOKED_BEEF)
                    .name("&cHunger: &7" + hunger + "/20")
                    .amount(hunger == 0 ? 1 : hunger)
                    .build();
        }

    }

    @AllArgsConstructor
    private static class EffectsButton extends Button {

        private final Collection<PotionEffect> effects;

        @Override
        public ItemStack getButtonItem(Player player) {
            ItemBuilder builder=new ItemBuilder(Material.POTION).name("&c&lPotion Effects");

            if (effects.isEmpty()) {
                builder.lore("&cNo potion effects");
            } else {
                List<String> lore=new ArrayList<>();

                effects.forEach(effect -> {
                    String name = PotionUtil.getName(effect.getType()) + " " + (effect.getAmplifier() + 1);
                    String duration = " (" + TimeUtil.millisToTimer((effect.getDuration() / 20) * 1000L) + ")";
                    lore.add("&c" + name + "&f" + duration);
                });

                builder.lore(lore);
            }

            return builder.build();
        }

    }

    @AllArgsConstructor
    private static class PotionsButton extends Button {

        private final String name;
        private final int potions;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.POTION)
                    .durability(16421)
                    .amount(potions == 0 ? 1 : potions)
                    .name("&dPotions")
                    .lore("&f" + name + " had " + potions + " potion" + (potions == 1 ? "" : "s") + " left.")
                    .build();
        }

    }

    @AllArgsConstructor
    private static class StatisticsButton extends Button {

        private final TeamPlayer teamPlayer;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.PAPER)
                    .name("&aStatistics")
                    .lore(Arrays.asList(
                            "&fTotal Hits: &a" + teamPlayer.getHits(),
                            "&fLongest Combo: &a" + teamPlayer.getLongestCombo(),
                            "&fPotions Thrown: &a" + teamPlayer.getPotionsThrown(),
                            "&fPotions Missed: &a" + teamPlayer.getPotionsMissed(),
                            "&fPotion Accuracy: &a" + teamPlayer.getPotionAccuracy() + "%"
                    ))
                    .build();
        }

    }

}
