package xyz.refinedev.practice.party.menu;

import org.bukkit.entity.Player;
import xyz.refinedev.practice.Array;
import xyz.refinedev.practice.party.Party;
import xyz.refinedev.practice.party.menu.buttons.PartyDuelButton;
import xyz.refinedev.practice.profile.Profile;
import xyz.refinedev.practice.util.menu.Button;
import xyz.refinedev.practice.util.menu.pagination.PaginatedMenu;

import java.util.*;

public class PartyDuelMenu extends PaginatedMenu {

    private final Array plugin = this.getPlugin();

    public PartyDuelMenu() {
        this.setPlaceholder(true);
        this.setAutoUpdate(true);
    }

    @Override
    public String getPrePaginatedTitle(Player player) {
        return "&7Other Parties";
    }
    
    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        List<Party> parties = new ArrayList<>(plugin.getPartyManager().getParties());
        int index = 0;

        parties.sort(Comparator.comparing(p -> p.getPlayers().size()));

        for (Party party : parties) {
            Profile profile = plugin.getProfileManager().getByUUID(party.getLeader().getUniqueId());
            if (party.isMember(player.getUniqueId()) || !profile.getSettings().isReceiveDuelRequests()) continue;

            buttons.put(index++, new PartyDuelButton(party));
        }
        return buttons;
    }

    @Override
    public int getSize() {
        return 9 * 6;
    }

    @Override
    public int getMaxItemsPerPage(Player player) {
        return 9 * 5; // top row is dedicated to switching
    }
}
