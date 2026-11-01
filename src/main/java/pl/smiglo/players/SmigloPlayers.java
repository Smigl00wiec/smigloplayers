package pl.smiglo.players;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SmigloPlayers extends JavaPlugin implements PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";

    private final Map<String, Integer> serverCounts = new ConcurrentHashMap<>();
    private volatile int networkCount = -1;
    private List<String> modeServers = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        modeServers = new ArrayList<>(getConfig().getStringList("mode-servers"));

        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);

        long refresh = Math.max(10L, getConfig().getLong("refresh-ticks", 40L));
        Bukkit.getScheduler().runTaskTimer(this, this::requestCounts, 20L, refresh);

        boolean ok = new SmigloExpansion(this).register();
        if (!ok) {
            getLogger().warning("Nie udalo sie zarejestrowac placeholderow. "
                    + "Prawdopodobnie identyfikator '" + getConfig().getString("identifier", "smiglo")
                    + "' jest juz zajety przez inny plugin - zmien 'identifier' w config.yml.");
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    /** Wysyla zapytania do proxy. Wymaga co najmniej jednego gracza online na serwerze. */
    private void requestCounts() {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) {
            return;
        }
        Player carrier = online.iterator().next();

        send(carrier, "ALL");
        for (String server : modeServers) {
            send(carrier, server);
        }
    }

    private void send(Player carrier, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF(server);
        carrier.sendPluginMessage(this, CHANNEL, out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String sub = in.readUTF();
            if (!"PlayerCount".equals(sub)) {
                return;
            }
            String server = in.readUTF();
            int count = in.readInt();
            if ("ALL".equalsIgnoreCase(server)) {
                networkCount = count;
            } else {
                serverCounts.put(server, count);
            }
        } catch (IOException ignored) {
        }
    }

    /** %<id>_players% - gracze na trybie. */
    public int getModeCount() {
        if (modeServers.isEmpty()) {
            return Bukkit.getOnlinePlayers().size();
        }
        int sum = 0;
        for (String server : modeServers) {
            sum += serverCounts.getOrDefault(server, 0);
        }
        return sum;
    }

    /** %<id>_playersall% - gracze na calej sieci. */
    public int getNetworkCount() {
        int c = networkCount;
        return c < 0 ? Bukkit.getOnlinePlayers().size() : c;
    }
}
