package me.drizzy.practice.kit.command;

import me.drizzy.practice.kit.Kit;
import me.drizzy.practice.util.CC;
import me.drizzy.practice.util.command.command.CommandMeta;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

@CommandMeta(label = "kit setloadout", permission = "array.dev")
public class KitSetLoadoutCommand {

    public void execute(Player player, Kit kit) {
        if (kit == null) {
            player.sendMessage(CC.RED + "A kit with that name does not exist.");
            return;
        }

        kit.getKitLoadout().setArmor(player.getInventory().getArmorContents());
        kit.getKitLoadout().setContents(player.getInventory().getContents());
        List<PotionEffect> potionEffects = new ArrayList<>();
        potionEffects.addAll(player.getActivePotionEffects());
        kit.getKitLoadout().setEffects(potionEffects);
        kit.save();

        player.sendMessage((CC.translate("&8[&b&lArray&8] &a")  + "You updated the kit's loadout."));
    }

}
