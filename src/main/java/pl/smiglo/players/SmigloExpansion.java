package pl.smiglo.players;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class SmigloExpansion extends PlaceholderExpansion {

    private final SmigloPlayers plugin;

    public SmigloExpansion(SmigloPlayers plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return plugin.getConfig().getString("identifier", "smiglo");
    }

    @Override
    public String getAuthor() {
        return "Smiglo";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        switch (params.toLowerCase()) {
            case "players":
                return String.valueOf(plugin.getModeCount());
            case "playersall":
                return String.valueOf(plugin.getNetworkCount());
            default:
                return null;
        }
    }
}
