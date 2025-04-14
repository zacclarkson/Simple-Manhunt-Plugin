// manhunt/src/main/java/com/clarkson/manhunt/Manhunt.java
package com.clarkson.manhunt;

// Import your command executor
import com.clarkson.manhunt.commands.ManhuntCommandExecutor;
import com.clarkson.manhunt.listeners.CompassListener;
import com.clarkson.manhunt.listeners.PlayerWorldChangeListener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // Import Objects for requireNonNull
import java.util.UUID;

public final class Manhunt extends JavaPlugin {

    private RoleManager roleManager;
    private final Map<UUID, Map<String, Location>> runnerLastLocations = new HashMap<>();
    private BukkitTask compassCheckTask;

    @Override
    public void onEnable() {
        getLogger().info("Manhunt plugin is enabling!");

        this.roleManager = new RoleManager();
        getLogger().info("RoleManager initialized.");

        // Register Listeners
        PluginManager pm = getServer().getPluginManager();
        CompassListener compassListener = new CompassListener(this, roleManager, runnerLastLocations);
        pm.registerEvents(compassListener, this);
        getLogger().info("CompassListener registered.");
        PlayerWorldChangeListener worldChangeListener = new PlayerWorldChangeListener(roleManager, runnerLastLocations);
        pm.registerEvents(worldChangeListener, this);
        getLogger().info("PlayerWorldChangeListener registered.");

        // Schedule Compass Check Task
        startCompassCheckTask();
        getLogger().info("Compass check task scheduled.");

        // --- Register Command Executor ---
        ManhuntCommandExecutor commandExecutor = new ManhuntCommandExecutor(roleManager);
        // Use Objects.requireNonNull to handle potential null from getCommand gracefully
        Objects.requireNonNull(getCommand("manhunt"), "Manhunt command not found in plugin.yml")
               .setExecutor(commandExecutor);
        // Also set the TabCompleter
         Objects.requireNonNull(getCommand("manhunt"), "Manhunt command not found in plugin.yml")
               .setTabCompleter(commandExecutor);
        getLogger().info("Manhunt command executor registered.");
        // --- End Command Registration ---

    }

    @Override
    public void onDisable() {
        getLogger().info("Manhunt plugin is disabling!");

        if (compassCheckTask != null && !compassCheckTask.isCancelled()) {
            compassCheckTask.cancel();
            getLogger().info("Compass check task cancelled.");
        }

        if (roleManager != null) {
            roleManager.clearRoles();
        }
        runnerLastLocations.clear();
    }

    private void startCompassCheckTask() {
        long delayTicks = 100L;
        long periodTicks = 200L;
        this.compassCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (roleManager.isHunter(player)) {
                        if (!RoleManager.hasTrackerCompass(player)) {
                            ItemStack newCompass = CompassListener.createTrackerCompass();
                            player.getInventory().addItem(newCompass);
                            player.sendMessage(Component.text("Your Runner Tracker has been restored!")
                                                    .color(NamedTextColor.GOLD));
                            getLogger().info("Restored tracker compass for hunter: " + player.getName());
                        }
                    }
                }
            }
        }.runTaskTimer(this, delayTicks, periodTicks);
    }

    // Getters (unchanged)
    public RoleManager getRoleManager() { return roleManager; }
    public Map<UUID, Map<String, Location>> getRunnerLastLocations() { return runnerLastLocations; }
}
