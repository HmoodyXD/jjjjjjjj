package me.drizzy.practice.kit.command;

import me.drizzy.practice.kit.Kit;
import me.drizzy.practice.util.CC;
import me.drizzy.practice.util.command.command.CPL;
import me.drizzy.practice.util.command.command.CommandMeta;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandMeta(label={"kit stickspawn"}, permission = "practice.dev")
public class KitStickSpawnCommand {
    public void execute(Player player, @CPL("kit") Kit kit) {
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit does not exist");
        } else {
            if (kit.getGameRules().isStickspawn()
            ) {
                kit.getGameRules().setStickspawn(false);
            } else if (!kit.getGameRules().isStickspawn()) {
                kit.getGameRules().setStickspawn(true);
            }
            kit.save();
            player.sendMessage((CC.translate("&8[&b&lArray&8] &a")) + "Kit set stickspawn mode to " + (kit.getGameRules().isStickspawn() ? "true!" : "false!"));
        }
    }
}