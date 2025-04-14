// manhunt/src/main/java/com/clarkson/manhunt/listeners/PlayerWorldChangeListener.java
package com.clarkson.manhunt.listeners;

import com.clarkson.manhunt.RoleManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for players changing worlds and updates their last known location
 * if they are a runner.
 */
public class PlayerWorldChangeListener implements Listener {

    private final RoleManager roleManager;
    private final Map<UUID, Map<String, Location>> runnerLastLocations; // Reference to the map in Manhunt.java

    /**
     * Constructor for PlayerWorldChangeListener.
     * @param roleManager The RoleManager instance.
     * @param runnerLastLocations The map storing runner last locations per world.
     */
    public PlayerWorldChangeListener(RoleManager roleManager, Map<UUID, Map<String, Location>> runnerLastLocations) {
        this.roleManager = roleManager;
        this.runnerLastLocations = runnerLastLocations;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        World worldFrom = event.getFrom(); // The world the player just left

        // Check if the player is a runner
        if (roleManager.isRunner(playerUUID)) {
            // Record their location just before they left the previous world
            Location lastLocation = player.getLocation().clone(); // Clone location before world change potentially affects it

            // Get the map for this specific player, or create it if it doesn't exist
            runnerLastLocations.computeIfAbsent(playerUUID, k -> new HashMap<>());

            // Store the last location in the specific world they left
            runnerLastLocations.get(playerUUID).put(worldFrom.getName(), lastLocation);

            // Optional: Log this event
            // plugin.getLogger().info("Recorded last location for runner " + player.getName() + " in world " + worldFrom.getName());
        }
    }
}
