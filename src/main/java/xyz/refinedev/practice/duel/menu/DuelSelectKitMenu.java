package xyz.refinedev.practice.duel.menu;

import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import xyz.refinedev.practice.Array;
import xyz.refinedev.practice.arena.Arena;
import xyz.refinedev.practice.kit.Kit;
import xyz.refinedev.practice.profile.Profile;
import xyz.refinedev.practice.util.chat.CC;
import xyz.refinedev.practice.util.inventory.ItemBuilder;
import xyz.refinedev.practice.util.menu.Button;
import xyz.refinedev.practice.util.menu.Menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class DuelSelectKitMenu extends Menu {

    private final Array plugin = this.getPlugin();

    @Override
    public String getTitle(Player player) {
        return "&7Select a kit";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        boolean party = plugin.getProfileManager().getByUUID(player.getUniqueId()).getParty() != null;

        if (party) {
            if (Array.getInstance().getConfigHandler().isHCF_ENABLED()) {
                for ( Kit kit : plugin.getKitManager().getKits() ) {
                    if (kit.isEnabled() || kit.getName().equalsIgnoreCase("HCFTeamFight")) {
                        if (!kit.getGameRules().isTimed() && !kit.getGameRules().isBridge())
                            buttons.put(buttons.size(), new SelectKitButton(kit));
                    }
                }
            } else {
                for ( Kit kit : plugin.getKitManager().getKits() ) {
                    if (kit.isEnabled() && !kit.getName().equalsIgnoreCase("HCFTeamFight")) {
                        if (!kit.getGameRules().isTimed() && !kit.getGameRules().isBridge())
                            buttons.put(buttons.size(), new SelectKitButton(kit));
                    }
                }
            }
        } else {
            for ( Kit kit : plugin.getKitManager().getKits() ) {
                if (kit.isEnabled() && !kit.getName().equalsIgnoreCase("HCFTeamFight")) {
                    buttons.put(buttons.size(), new SelectKitButton(kit));
                }
            }
        }

        return buttons;
    }

    @Override
    public void onClose(Player player) {
        if (!isClosedByMenu()) {
            Profile profile = plugin.getProfileManager().getByUUID(player.getUniqueId());
            profile.setDuelProcedure(null);
        }
    }

    @AllArgsConstructor
    private class SelectKitButton extends Button {

        private final Kit kit;

        @Override
        public ItemStack getButtonItem(Player player) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("&cClick to send a duel with this kit.");
            return new ItemBuilder(kit.getDisplayIcon())
                    .name(kit.getDisplayName()).lore(lore)
                    .clearFlags()
                    .build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            Profile profile = plugin.getProfileManager().getByUUID(player.getUniqueId());


            if (profile.getDuelProcedure() == null) {
                player.sendMessage(CC.RED + "Could not find duel data.");
                return;
            }

            Arena arena = plugin.getArenaManager().getByKit(kit);

            profile.getDuelProcedure().setKit(kit);
            profile.getDuelProcedure().setArena(arena);

            player.closeInventory();

            if (player.hasPermission("array.donator")) {
                new DuelSelectArenaMenu().openMenu(player);
            } else {
                profile.getDuelProcedure().send();
            }
        }

    }

}
