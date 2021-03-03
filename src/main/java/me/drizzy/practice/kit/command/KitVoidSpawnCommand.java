package me.drizzy.practice.kit.command;

import me.drizzy.practice.kit.Kit;
import me.drizzy.practice.util.CC;
import me.drizzy.practice.util.command.command.CPL;
import me.drizzy.practice.util.command.command.CommandMeta;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandMeta(label={"kit voidspawn"}, permission = "array.dev")
public class KitVoidSpawnCommand {
    public void execute(Player player, @CPL("kit") Kit kit) {
        if (kit == null) {
            player.sendMessage(ChatColor.RED + "Kit does not exist");
        } else {
            if (kit.getGameRules().isVoidspawn()) {
                kit.getGameRules().setVoidspawn(false);
            } else if (!kit.getGameRules().isVoidspawn()) {
                kit.getGameRules().setVoidspawn(true);
            }
            kit.save();
            player.sendMessage((CC.translate("&8[&b&lArray&8] &a")) + "Kit set voidspawn mode to " + (kit.getGameRules().isVoidspawn() ? "true!" : "false!"));
        }
    }
}
