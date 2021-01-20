package nl.knokko.rememberhealth;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class RememberHealth extends JavaPlugin implements Listener {

    private File getStorageFile() {
        return new File(getDataFolder() + "/health.bin");
    }

    private void loadData() {
        File target = getStorageFile();
        if (target.exists()) {
            try {
                DataInputStream input = new DataInputStream(Files.newInputStream(target.toPath()));
                int numEntries = input.readInt();
                playersHealth = new HashMap<>(numEntries);

                for (int counter = 0; counter < numEntries; counter++) {
                    UUID id = new UUID(input.readLong(), input.readLong());
                    double health = input.readDouble();
                    playersHealth.put(id, health);
                }

                input.close();
            } catch (IOException ioTrouble) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to read the health of the players", ioTrouble);
            }
        } else {
            Bukkit.getLogger().warning("Couldn't find health.bin. This is fine if you use this plug-in for the first time.");
            playersHealth = new HashMap<>();
        }
    }

    private void saveData() {
        getDataFolder().mkdirs();
        File target = getStorageFile();
        try {
            DataOutputStream output = new DataOutputStream(Files.newOutputStream(target.toPath()));
            output.writeInt(playersHealth.size());

            for (Map.Entry<UUID, Double> entry : playersHealth.entrySet()) {
                output.writeLong(entry.getKey().getMostSignificantBits());
                output.writeLong(entry.getKey().getLeastSignificantBits());
                output.writeDouble(entry.getValue());
            }

            output.flush();
            output.close();
        } catch (IOException ioTrouble) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save the health of the players", ioTrouble);
        }
    }

    private Map<UUID, Double> playersHealth;

    @Override
    public void onEnable() {
        loadData();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playersHealth.put(player.getUniqueId(), player.getHealth());
        }
        saveData();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            UUID id = player.getUniqueId();
            Double lastHealth = playersHealth.get(id);

            if (lastHealth != null) {
                player.setHealth(lastHealth);
                playersHealth.remove(id);
            }
        }, 2); // Apparently, Bukkit needs 1 extra tick to fix its attribute modifiers
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playersHealth.put(player.getUniqueId(), player.getHealth());
    }
}
