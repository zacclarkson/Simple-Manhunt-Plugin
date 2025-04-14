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

    private final Manhunt plugin;
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
    public CompassListener(Manhunt plugin, RoleManager roleManager, Map<UUID, Map<String, Location>> runnerLastLocations) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.runnerLastLocations = runnerLastLocations; // Store the map reference
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player hunter = event.getPlayer();

        // --- Standard Checks ---
        if (!roleManager.isHunter(hunter)) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack itemInHand = hunter.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.COMPASS) return;
        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(TRACKER_COMPASS_KEY, PersistentDataType.BYTE)) return;
        // --- End Checks ---

        // Find the best target (direct or indirect) and point the compass
        findAndPointToRunner(hunter);

        // Optional: Prevent default compass behavior
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

        Player nearestRunnerDirect = null;
        double minDirectDistanceSq = Double.MAX_VALUE;

        Player nearestRunnerIndirect = null;
        Location nearestIndirectLocation = null;
        double minIndirectDistanceSq = Double.MAX_VALUE;

        for (UUID runnerUUID : roleManager.getRunners()) {
            Player runner = Bukkit.getPlayer(runnerUUID);

            if (runner != null && runner.isOnline()) {
                Location runnerLocation = runner.getLocation();

                if (hunterWorld.equals(runner.getWorld())) {
                    double distanceSq = hunterLocation.distanceSquared(runnerLocation);
                    if (distanceSq < minDirectDistanceSq) {
                        minDirectDistanceSq = distanceSq;
                        nearestRunnerDirect = runner;
                    }
                } else {
                    Map<String, Location> runnerWorlds = runnerLastLocations.get(runnerUUID);
                    if (runnerWorlds != null) {
                        Location lastKnownLoc = runnerWorlds.get(hunterWorldName);
                        if (lastKnownLoc != null && lastKnownLoc.getWorld().equals(hunterWorld)) {
                            double distanceSq = hunterLocation.distanceSquared(lastKnownLoc);
                            if (distanceSq < minIndirectDistanceSq) {
                                minIndirectDistanceSq = distanceSq;
                                nearestIndirectLocation = lastKnownLoc;
                                nearestRunnerIndirect = runner;
                            }
                        }
                    }
                }
            }
        }

        // --- Determine Final Compass Target and Send Message (NO DISTANCE) ---

        if (nearestRunnerDirect != null) {
            // Priority 1: Found a runner in the same world
            hunter.setCompassTarget(nearestRunnerDirect.getLocation());
            // Send message WITHOUT distance
            hunter.sendMessage(
                Component.text("Tracking ").color(NamedTextColor.GREEN)
                    .append(Component.text(nearestRunnerDirect.getName()).color(NamedTextColor.WHITE)) // Runner's name
                    .append(Component.text(" directly.").color(NamedTextColor.GREEN)) // Updated message
            );
        } else if (nearestIndirectLocation != null && nearestRunnerIndirect != null) {
            // Priority 2: No runners in this world, but found a last known location
            hunter.setCompassTarget(nearestIndirectLocation);
            // Send message WITHOUT distance
            hunter.sendMessage(
                Component.text("Tracking towards ").color(NamedTextColor.YELLOW)
                    .append(Component.text(nearestRunnerIndirect.getName()).color(NamedTextColor.WHITE)) // Runner's name
                    .append(Component.text("'s last known location.").color(NamedTextColor.YELLOW)) // Updated message
            );
        } else {
            // Priority 3: No runners found online, or no runners ever recorded in this dimension
            hunter.setCompassTarget(hunterWorld.getSpawnLocation());
            hunter.sendMessage(Component.text("No runners found in your current dimension or their location is unknown.").color(NamedTextColor.RED));
        }
    }
}
