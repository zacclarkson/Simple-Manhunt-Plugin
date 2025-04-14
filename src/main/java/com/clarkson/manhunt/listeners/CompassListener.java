// manhunt/src/main/java/com/clarkson/manhunt/listeners/CompassListener.java
package com.clarkson.manhunt.listeners;

import com.clarkson.manhunt.Manhunt;
import com.clarkson.manhunt.RoleManager;
import net.kyori.adventure.text.Component; // Import Adventure Component
import net.kyori.adventure.text.format.NamedTextColor; // Import Adventure NamedTextColor
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

import java.util.UUID;

public class CompassListener implements Listener {

    private final Manhunt plugin;
    private final RoleManager roleManager;

    // Define a unique key for your compass tag
    public static final NamespacedKey TRACKER_COMPASS_KEY = new NamespacedKey("manhunt", "tracker_compass");

    public CompassListener(Manhunt plugin, RoleManager roleManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player hunter = event.getPlayer();

        if (!roleManager.isHunter(hunter)) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack itemInHand = hunter.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.COMPASS) {
            return;
        }

        ItemMeta meta = itemInHand.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(TRACKER_COMPASS_KEY, PersistentDataType.BYTE)) {
            // Optional: Send message using Adventure API
            // hunter.sendMessage(Component.text("This compass doesn't seem to be attuned for tracking.").color(NamedTextColor.YELLOW));
            return;
        }

        findAndPointToNearestRunner(hunter);
        // event.setCancelled(true); // Optional
    }

    private void findAndPointToNearestRunner(Player hunter) {
        Location hunterLocation = hunter.getLocation();
        Player nearestRunner = null;
        double minDistanceSquared = Double.MAX_VALUE;

        for (UUID runnerUUID : roleManager.getRunners()) {
            Player runner = Bukkit.getPlayer(runnerUUID);

            if (runner != null && runner.isOnline() && hunter.getWorld().equals(runner.getWorld())) {
                Location runnerLocation = runner.getLocation();
                double distanceSquared = hunterLocation.distanceSquared(runnerLocation);

                if (distanceSquared < minDistanceSquared) {
                    minDistanceSquared = distanceSquared;
                    nearestRunner = runner;
                }
            }
        }

        if (nearestRunner != null) {
            hunter.setCompassTarget(nearestRunner.getLocation());
            double distance = Math.sqrt(minDistanceSquared);
            // Send message using Adventure API
            hunter.sendMessage(
                Component.text("Compass pointing towards ")
                    .color(NamedTextColor.GREEN)
                    .append(Component.text(nearestRunner.getName()).color(NamedTextColor.WHITE)) // Append runner's name
                    .append(Component.text(" ("))
                    .append(Component.text(String.format("%.1f", distance))) // Append distance
                    .append(Component.text(" blocks away)."))
            );
        } else {
            hunter.setCompassTarget(hunter.getWorld().getSpawnLocation());
            // Send message using Adventure API
            hunter.sendMessage(Component.text("No runners found in your current dimension.").color(NamedTextColor.RED));
        }
    }
}
