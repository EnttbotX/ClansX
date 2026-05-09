package x.Entt.ClansX.Utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import x.Entt.ClansX.CX;

public class PAPI extends PlaceholderExpansion {

    private final CX plugin;

    public PAPI(CX plugin) {
        this.plugin = plugin;
    }

    @NotNull
    @Override
    public String getIdentifier() { return "cx"; }

    @NotNull
    @Override
    public String getAuthor() {
        return "EnttbotX";
    }

    @NotNull
    @Override
    public String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public boolean canRegister() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "no player";

        FileConfiguration pData = plugin.getFH().getPlayerData(player.getUniqueId());
        String clanName = pData.getString("clan");
        if (clanName == null) return "no clan";

        FileConfiguration clan = plugin.getFH().getClan(clanName);
        if (clan == null) return "no clan file";

        return switch (identifier.toLowerCase()) {
            case "player_balance" -> String.valueOf(CX.getEcon().getBalance(player));
            case "clan_name" -> clanName;
            case "clan_leader" -> clan.getString("leader", "N/A");
            case "clan_founder" -> clan.getString("founder", "N/A");
            case "clan_balance" -> String.valueOf(clan.getInt("balance", 0));
            case "clan_kills" -> String.valueOf(clan.getInt("kills", 0));
            case "clan_war_streak" -> String.valueOf(clan.getInt("wars.streak", 0));
            case "clan_prestige" -> String.valueOf(clan.getInt("prestige", 0));
            case "clan_prestige_level" -> {
                int prestige = clan.getInt("prestige", 0);
                int xpPerLevel = plugin.getFH().getConfig().getInt("clans.prestige.acquire-new-prestige", 10000);
                yield String.valueOf(xpPerLevel > 0 ? prestige / xpPerLevel : 0);
            }
            case "clan_war_won" -> String.valueOf(clan.getStringList("wars.won").size());
            case "clan_war_defeated" -> String.valueOf(clan.getStringList("wars.defeated").size());
            case "clan_war_active" -> String.valueOf(clan.getStringList("wars.active").size());
            case "clan_war_total" -> {
                int total = clan.getStringList("wars.active").size()
                        + clan.getStringList("wars.won").size()
                        + clan.getStringList("wars.defeated").size();
                yield String.valueOf(total);
            }
            case "clan_membercount" -> String.valueOf(clan.getStringList("users").size());
            case "clan_membercount_online" -> {
                long online = clan.getStringList("users").stream()
                        .filter(u -> Bukkit.getPlayerExact(u) != null).count();
                yield String.valueOf(online);
            }
            case "clan_membercount_offline" -> {
                long offline = clan.getStringList("users").size() -
                        clan.getStringList("users").stream()
                                .filter(u -> Bukkit.getPlayerExact(u) != null).count();
                yield String.valueOf(offline);
            }

            default -> "&5&lEnttbot&d&lX";
        };
    }

    public void registerPlaceholders() {
        if (!isRegistered() && !register()) {
            plugin.getLogger().warning("Failed to register ClansX placeholders.");
        } else {
            plugin.getLogger().info("ClansX placeholders registered.");
        }
    }
}