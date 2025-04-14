// manhunt/src/main/java/com/clarkson/manhunt/Manhunt.java
package com.clarkson.manhunt;

import com.clarkson.manhunt.listeners.CompassListener;
import com.clarkson.manhunt.listeners.PlayerWorldChangeListener;
import com.clarkson.manhunt.RoleManager; // Import RoleManager to use its helper methods

import net.kyori.adventure.text.Component; // Import Adventure Component
import net.kyori.adventure.text.format.NamedTextColor; // Import Adventure NamedTextColor

import org.bukkit.Bukkit; // Import Bukkit
import org.bukkit.Location;
import org.bukkit.entity.Player; // Import Player
import org.bukkit.inventory.ItemStack; // Import ItemStack
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable; // Import BukkitRunnable
import org.bukkit.scheduler.BukkitTask; // Import BukkitTask

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Manhunt extends JavaPlugin {

    private RoleManager roleManager;
    private final Map<UUID, Map<String, Location>> runnerLastLocations = new HashMap<>();
    private BukkitTask compassCheckTask; // Store the task to cancel it later

    @Override
    public void onEnable() {
        getLogger().info("Manhunt plugin is enabling!");

        this.roleManager = new RoleManager();
        getLogger().info("RoleManager initialized.");

        PluginManager pm = getServer().getPluginManager();

        CompassListener compassListener = new CompassListener(this, roleManager, runnerLastLocations);
        pm.registerEvents(compassListener, this);
        getLogger().info("CompassListener registered.");

        PlayerWorldChangeListener worldChangeListener = new PlayerWorldChangeListener(roleManager, runnerLastLocations);
        pm.registerEvents(worldChangeListener, this);
        getLogger().info("PlayerWorldChangeListener registered.");

        // --- Schedule the Repeating Compass Check Task ---
        startCompassCheckTask();
        getLogger().info("Compass check task scheduled.");
        // --- End Scheduling ---

    }

    @Override
    public void onDisable() {
        getLogger().info("Manhunt plugin is disabling!");

        // --- Cancel the repeating task ---
        if (compassCheckTask != null && !compassCheckTask.isCancelled()) {
            compassCheckTask.cancel();
            getLogger().info("Compass check task cancelled.");
        }
        // --- End Cancel ---

        if (roleManager != null) {
            // Ensure compasses are removed when roles are cleared on disable
            roleManager.clearRoles();
        }
        runnerLastLocations.clear();
    }

    // --- Method to start the compass check task ---
    private void startCompassCheckTask() {
        // Run task every 10 seconds (200 ticks), starting after 5 seconds (100 ticks)
        long delayTicks = 100L;
        long periodTicks = 200L;

        this.compassCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Iterate through all online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Check if the player is supposed to be a hunter
                    if (roleManager.isHunter(player)) {
                        // Check if they actually have the compass in their inventory
                        if (!RoleManager.hasTrackerCompass(player)) {
                            // Hunter is missing the compass, give them a new one
                            ItemStack newCompass = CompassListener.createTrackerCompass();
                            player.getInventory().addItem(newCompass);

                            // Notify the hunter (optional)
                            player.sendMessage(Component.text("Your Runner Tracker has been restored!")
                                                    .color(NamedTextColor.GOLD));
                            getLogger().info("Restored tracker compass for hunter: " + player.getName()); // Log for server console
                        }
                    }
                }
            }
        }.runTaskTimer(this, delayTicks, periodTicks); // Use runTaskTimer for synchronous inventory access
    }
    // --- End Task Method ---


    // Getters (unchanged)
    public RoleManager getRoleManager() { return roleManager; }
    public Map<UUID, Map<String, Location>> getRunnerLastLocations() { return runnerLastLocations; }
}
