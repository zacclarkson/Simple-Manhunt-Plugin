// manhunt/src/main/java/com/clarkson/manhunt/Manhunt.java
package com.clarkson.manhunt;

import com.clarkson.manhunt.listeners.CompassListener;
import com.clarkson.manhunt.listeners.PlayerWorldChangeListener; // Import new listener
import org.bukkit.Location;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Manhunt extends JavaPlugin {

    private RoleManager roleManager;
    // Map structure: Runner UUID -> World Name -> Last Location in that World
    private final Map<UUID, Map<String, Location>> runnerLastLocations = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("Manhunt plugin is enabling!");

        // 1. Create RoleManager instance
        this.roleManager = new RoleManager();
        getLogger().info("RoleManager initialized.");

        // 2. Create and Register Listeners
        PluginManager pm = getServer().getPluginManager();

        // Register CompassListener (pass location map)
        CompassListener compassListener = new CompassListener(roleManager, runnerLastLocations);
        pm.registerEvents(compassListener, this);
        getLogger().info("CompassListener registered.");

        // Register PlayerWorldChangeListener (pass role manager and location map)
        PlayerWorldChangeListener worldChangeListener = new PlayerWorldChangeListener(roleManager, runnerLastLocations);
        pm.registerEvents(worldChangeListener, this);
        getLogger().info("PlayerWorldChangeListener registered.");

        // Optional: Add commands here later
    }

    @Override
    public void onDisable() {
        getLogger().info("Manhunt plugin is disabling!");
        if (roleManager != null) {
            roleManager.clearRoles();
        }
        runnerLastLocations.clear(); // Clear locations on disable
    }

    // Getter for RoleManager (if needed)
    public RoleManager getRoleManager() {
        return roleManager;
    }

    // Getter for Location Map (might not be needed externally, but good practice)
    public Map<UUID, Map<String, Location>> getRunnerLastLocations() {
        return runnerLastLocations;
    }
}
