// manhunt/src/main/java/com/clarkson/manhunt/Manhunt.java
package com.clarkson.manhunt;

// Import your command executor AND the new command wrapper
import com.clarkson.manhunt.commands.ManhuntCommand;
import com.clarkson.manhunt.commands.ManhuntCommandExecutor;
import com.clarkson.manhunt.listeners.CompassListener;
import com.clarkson.manhunt.listeners.PlayerWorldChangeListener;
import com.clarkson.manhunt.RoleManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandMap; // Import CommandMap
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List; // Import List for aliases
import java.util.Map;
// import java.util.Objects; // No longer needed for getCommand
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

        // Register Listeners (unchanged)
        PluginManager pm = getServer().getPluginManager();
        CompassListener compassListener = new CompassListener(this, roleManager, runnerLastLocations);
        pm.registerEvents(compassListener, this);
        getLogger().info("CompassListener registered.");
        PlayerWorldChangeListener worldChangeListener = new PlayerWorldChangeListener(roleManager, runnerLastLocations);
        pm.registerEvents(worldChangeListener, this);
        getLogger().info("PlayerWorldChangeListener registered.");

        // Schedule Compass Check Task (unchanged)
        startCompassCheckTask();
        getLogger().info("Compass check task scheduled.");

        // --- Register Command Programmatically ---
        registerManhuntCommand();
        getLogger().info("Manhunt command registered programmatically.");
        // --- End Command Registration ---

    }

    // --- Method to register the command ---
    private void registerManhuntCommand() {
        // 1. Create the executor/completer instance
        ManhuntCommandExecutor commandHandler = new ManhuntCommandExecutor(roleManager);

        // 2. Create the wrapper command instance
        ManhuntCommand manhuntCmd = new ManhuntCommand(
                "manhunt",                                  // Command name
                "Manages the Manhunt game roles.",          // Description
                "/<command> help",                          // Usage message (can be simple)
                List.of("mh"),                              // Aliases (e.g., /mh)
                commandHandler,                             // The executor instance
                commandHandler                              // The tab completer instance
        );

        // 3. Get the server's command map
        CommandMap commandMap = Bukkit.getCommandMap();

        // 4. Register the command
        // The first argument is a fallback prefix (usually your plugin name)
        // The second argument is the command object itself
        commandMap.register("manhunt", getName(), manhuntCmd); // Use plugin name as fallback prefix
    }
    // --- End Command Registration Method ---


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

        // Optional: Unregister command from CommandMap on disable?
        // Usually not strictly necessary, but can be done for completeness
        // CommandMap commandMap = Bukkit.getCommandMap();
        // if (commandMap != null && commandMap.getCommand("manhunt") instanceof ManhuntCommand) {
        //    try {
        //        // Accessing knownCommands map via reflection is needed to truly unregister
        //        // This is complex and often omitted. Server restart handles cleanup.
        //        getLogger().info("Manhunt command unregistered (basic).");
        //    } catch (Exception e) {
        //        getLogger().warning("Could not fully unregister command: " + e.getMessage());
        //    }
        // }
    }

    // startCompassCheckTask method (unchanged)
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
