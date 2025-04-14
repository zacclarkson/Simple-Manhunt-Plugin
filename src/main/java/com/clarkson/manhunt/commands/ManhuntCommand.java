package com.clarkson.manhunt.commands; // Same package as your executor

import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wrapper class to register the Manhunt command programmatically for Paper compatibility.
 * Delegates execution and tab completion to the actual CommandExecutor/TabCompleter.
 */
public class ManhuntCommand extends Command {

    private final CommandExecutor executor;
    private final TabCompleter completer;

    public ManhuntCommand(String name, String description, String usageMessage, List<String> aliases, CommandExecutor executor, TabCompleter completer) {
        super(name, description, usageMessage, aliases);
        this.executor = executor;
        this.completer = completer;

        // Set permission directly on the command object
        this.setPermission("manhunt.admin");
        // Handle permission message manually in the execute method
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        // Delegate execution to the provided executor
        if (!sender.hasPermission(this.getPermission())) {
            sender.sendMessage("You don't have permission to use this command.");
            return false;
        }
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        // Delegate tab completion to the provided completer
        return completer.onTabComplete(sender, this, alias, args);
    }
}
