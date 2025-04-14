// manhunt/src/main/java/com/clarkson/manhunt/listeners/CompassListener.java
package com.clarkson.manhunt.listeners;

import com.clarkson.manhunt.Manhunt;
import com.clarkson.manhunt.RoleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
// Import TextDecoration if you are using it for item names/lore
// import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;

public class CompassListener implements Listener {

    private final RoleManager roleManager;
    // Reference to the map holding last known locations, passed from Manhunt class
    private final Map<UUID, Map<String, Location>> runnerLastLocations;

    // Key to identify the special tracking compass
    public static final NamespacedKey TRACKER_COMPASS_KEY = new NamespacedKey("manhunt", "tracker_compass");

    /**
     * Constructor for CompassListener.
     * Now requires the runnerLastLocations map.
     * @param plugin The main plugin instance.
     * @param roleManager The RoleManager instance.
     * @param runnerLastLocations The map storing runner last locations per world.
     */
    public CompassListener(RoleManager roleManager, Map<UUID, Map<String, Location>> runnerLastLocations) {
        this.roleManager = roleManager;
        this.runnerLastLocations = runnerLastLocations; // Store the map reference
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player hunter = event.getPlayer();

        // --- Standard Checks ---
        // 1. Is the player a Hunter?
        if (!roleManager.isHunter(hunter)) {
            return;
        }
        // 2. Was it a right-click action?
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        // 3. Are they holding a compass?
        ItemStack itemInHand = hunter.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.COMPASS) {
            return;
        }
        // 4. Does the compass have the special tag?
        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null) {
            return; // Should have meta, safety check
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(TRACKER_COMPASS_KEY, PersistentDataType.BYTE)) {
            return; // Not the special tracking compass
        }
        // --- End Checks ---


        // --- Updated Compass Logic ---
        // Find the best target (direct or indirect) and point the compass
        findAndPointToRunner(hunter);

        // Optional: Prevent default compass behavior (e.g., interacting with lodestones)
        // event.setCancelled(true);
    }

    /**
     * Finds the nearest online runner (directly if in the same world,
     * or via last known location if in a different world) and updates the hunter's compass.
     * @param hunter The Hunter whose compass needs updating.
     */
    private void findAndPointToRunner(Player hunter) {
        Location hunterLocation = hunter.getLocation();
        World hunterWorld = hunter.getWorld();
        String hunterWorldName = hunterWorld.getName();

        // Variables to store the best direct tracking target (same world)
        Player nearestRunnerDirect = null;
        double minDirectDistanceSq = Double.MAX_VALUE;

        // Variables to store the best indirect tracking target (different world, using last known location)
        Player nearestRunnerIndirect = null; // Store the player for the message
        Location nearestIndirectLocation = null; // Store the location to point the compass at
        double minIndirectDistanceSq = Double.MAX_VALUE;

        // Iterate through all players assigned the Runner role
        for (UUID runnerUUID : roleManager.getRunners()) {
            Player runner = Bukkit.getPlayer(runnerUUID); // Get the online player instance

            // Only consider runners who are currently online
            if (runner != null && runner.isOnline()) {
                Location runnerLocation = runner.getLocation(); // Current location of the runner

                // --- Case 1: Runner is in the SAME world as the Hunter ---
                if (hunterWorld.equals(runner.getWorld())) {
                    double distanceSq = hunterLocation.distanceSquared(runnerLocation);
                    // If this runner is closer than the current best direct target, update
                    if (distanceSq < minDirectDistanceSq) {
                        minDirectDistanceSq = distanceSq;
                        nearestRunnerDirect = runner;
                    }
                }
                // --- Case 2: Runner is in a DIFFERENT world ---
                else {
                    // Check if we have stored a last known location for this runner
                    // in the hunter's current world dimension.
                    Map<String, Location> runnerWorlds = runnerLastLocations.get(runnerUUID);
                    if (runnerWorlds != null) {
                        // Retrieve the last known location for the specific world the hunter is in
                        Location lastKnownLoc = runnerWorlds.get(hunterWorldName);

                        // Check if a location was found AND it's valid for the hunter's current world
                        // (This check prevents potential issues if the world data is somehow invalid)
                        if (lastKnownLoc != null && lastKnownLoc.getWorld().equals(hunterWorld)) {
                            double distanceSq = hunterLocation.distanceSquared(lastKnownLoc);
                            // If this last known location is closer than the current best indirect target, update
                            if (distanceSq < minIndirectDistanceSq) {
                                minIndirectDistanceSq = distanceSq;
                                nearestIndirectLocation = lastKnownLoc; // This is the location to point at
                                nearestRunnerIndirect = runner; // Keep track of which runner this location belongs to for the message
                            }
                        }
                    }
                }
            }
        }

        // --- Determine Final Compass Target and Send Message ---

        // Priority 1: If we found a direct target (runner in the same world), use that.
        if (nearestRunnerDirect != null) {
            hunter.setCompassTarget(nearestRunnerDirect.getLocation());
            double distance = Math.sqrt(minDirectDistanceSq); // Calculate actual distance for message
            hunter.sendMessage(
                Component.text("Tracking ").color(NamedTextColor.GREEN)
                    .append(Component.text(nearestRunnerDirect.getName()).color(NamedTextColor.WHITE)) // Runner's name
                    .append(Component.text(" directly (").color(NamedTextColor.GREEN))
                    .append(Component.text(String.format("%.1f", distance))) // Distance
                    .append(Component.text(" blocks away).").color(NamedTextColor.GREEN))
            );
        }
        // Priority 2: If no direct target, but we found an indirect target (last known location).
        else if (nearestIndirectLocation != null && nearestRunnerIndirect != null) {
            hunter.setCompassTarget(nearestIndirectLocation); // Point to the stored location
            double distance = Math.sqrt(minIndirectDistanceSq); // Calculate distance to the last known spot
            hunter.sendMessage(
                Component.text("Tracking towards ").color(NamedTextColor.YELLOW)
                    .append(Component.text(nearestRunnerIndirect.getName()).color(NamedTextColor.WHITE)) // Runner's name
                    .append(Component.text("'s last known location (").color(NamedTextColor.YELLOW))
                     .append(Component.text(String.format("%.1f", distance))) // Distance
                    .append(Component.text(" blocks away).").color(NamedTextColor.YELLOW))
            );
        }
        // Priority 3: No runners found online, or no runners ever recorded in this dimension.
        else {
            hunter.setCompassTarget(hunterWorld.getSpawnLocation()); // Point compass to world spawn as a fallback
            hunter.sendMessage(Component.text("No runners found in your current dimension or their location is unknown.").color(NamedTextColor.RED));
        }
    }
}
