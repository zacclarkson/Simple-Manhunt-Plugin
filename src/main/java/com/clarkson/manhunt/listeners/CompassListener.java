// manhunt/src/main/java/com/clarkson/manhunt/listeners/CompassListener.java
package com.clarkson.manhunt.listeners;

import com.clarkson.manhunt.Manhunt;
import com.clarkson.manhunt.RoleManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration; // Import TextDecoration
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment; // Import Enchantment
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag; // Import ItemFlag
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List; // Import List
import java.util.Map;
import java.util.UUID;

public class CompassListener implements Listener {

    private final Manhunt plugin;
    private final RoleManager roleManager;
    private final Map<UUID, Map<String, Location>> runnerLastLocations;

    public static final NamespacedKey TRACKER_COMPASS_KEY = new NamespacedKey("manhunt", "tracker_compass");

    // --- Static Helper Method to Create the Compass ---
    /**
     * Creates the special Manhunt tracking compass item.
     * Includes name, lore, enchantment glint, and persistent data tag.
     * @return The configured ItemStack for the tracking compass.
     */
    public static ItemStack createTrackerCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();

        if (meta != null) {
            // Set Name (Adventure API)
            meta.displayName(
                Component.text("Runner Tracker")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false) // Disable italics
            );

            // Set Lore (Adventure API)
            meta.lore(List.of(
                Component.text("Right-click to track the nearest runner!")
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false) // Disable italics
            ));

            // Add Enchantment Glint (e.g., Unbreaking I)
            // The 'true' ignores level restrictions if needed, safe here.
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);

            // Optional: Hide the actual enchantment text if you only want the glint
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // Add the Persistent Data Tag to identify it
            meta.getPersistentDataContainer().set(TRACKER_COMPASS_KEY, PersistentDataType.BYTE, (byte) 1);

            compass.setItemMeta(meta); // Apply all changes
        }
        return compass;
    }
    // --- End Helper Method ---


    public CompassListener(Manhunt plugin, RoleManager roleManager, Map<UUID, Map<String, Location>> runnerLastLocations) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.runnerLastLocations = runnerLastLocations;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player hunter = event.getPlayer();

        // Standard Checks (unchanged)
        if (!roleManager.isHunter(hunter)) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack itemInHand = hunter.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.COMPASS) return;
        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        // Check uses the static key from this class now
        if (!container.has(TRACKER_COMPASS_KEY, PersistentDataType.BYTE)) return;

        findAndPointToRunner(hunter);
        // event.setCancelled(true); // Optional
    }

    // findAndPointToRunner method remains the same (without distance)
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

        if (nearestRunnerDirect != null) {
            hunter.setCompassTarget(nearestRunnerDirect.getLocation());
            hunter.sendMessage(
                Component.text("Tracking ").color(NamedTextColor.GREEN)
                    .append(Component.text(nearestRunnerDirect.getName()).color(NamedTextColor.WHITE))
                    .append(Component.text(" directly.").color(NamedTextColor.GREEN))
            );
        } else if (nearestIndirectLocation != null && nearestRunnerIndirect != null) {
            hunter.setCompassTarget(nearestIndirectLocation);
            hunter.sendMessage(
                Component.text("Tracking towards ").color(NamedTextColor.YELLOW)
                    .append(Component.text(nearestRunnerIndirect.getName()).color(NamedTextColor.WHITE))
                    .append(Component.text("'s last known location.").color(NamedTextColor.YELLOW))
            );
        } else {
            hunter.setCompassTarget(hunterWorld.getSpawnLocation());
            hunter.sendMessage(Component.text("No runners found in your current dimension or their location is unknown.").color(NamedTextColor.RED));
        }
    }
}
