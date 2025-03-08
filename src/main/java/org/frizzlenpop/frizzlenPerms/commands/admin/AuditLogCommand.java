package org.frizzlenpop.frizzlenPerms.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.frizzlenpop.frizzlenPerms.FrizzlenPerms;
import org.frizzlenpop.frizzlenPerms.commands.SubCommand;
import org.frizzlenpop.frizzlenPerms.audit.AuditManager;
import org.frizzlenpop.frizzlenPerms.models.AuditLog;
import org.frizzlenpop.frizzlenPerms.utils.MessageUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to view the permission audit logs.
 */
public class AuditLogCommand implements SubCommand {
    
    private final FrizzlenPerms plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Creates a new AuditLogCommand.
     *
     * @param plugin The plugin instance
     */
    public AuditLogCommand(FrizzlenPerms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "auditlog";
    }
    
    @Override
    public String getDescription() {
        return "Views the permission audit logs.";
    }
    
    @Override
    public String getUsage() {
        return "/frizzlenperms auditlog [page] [filters...]";
    }
    
    @Override
    public String getPermission() {
        return "frizzlenperms.admin.auditlog";
    }
    
    @Override
    public int getMinArgs() {
        return 0;
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("logs", "history");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Determine the page number first
        int pageNumber = 1; // Default page is 1
        Map<String, String> filters = new HashMap<>();
        
        // Parse arguments
        if (args.length > 0) {
            try {
                // Try to parse first arg as page number
                pageNumber = Integer.parseInt(args[0]);
                
                // Process filters from arg 1 onwards
                processFilters(args, 1, filters);
            } catch (NumberFormatException e) {
                // First arg is not a page number, treat all args as filters
                processFilters(args, 0, filters);
            }
        }
        
        // Now assign to the final variable - this happens only once
        final int page = pageNumber;
        
        // Validate page number
        if (page < 1) {
            MessageUtils.sendMessage(sender, "admin.auditlog-invalid-page");
            return false;
        }
        
        // Get logs asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            AuditManager auditManager = plugin.getAuditManager();
            List<AuditLog> logs;
            
            try {
                // Get logs with filters
                // Note: Since there's no direct method to get logs with filters,
                // we'll get all logs and filter them in-memory
                logs = auditManager.getAllAuditLogs(1000); // Use a reasonable limit
                
                // Apply filters if any
                if (!filters.isEmpty()) {
                    logs = logs.stream()
                            .filter(log -> matchesFilters(log, filters))
                            .collect(Collectors.toList());
                }
                
                // Sort logs by timestamp (newest first)
                logs = logs.stream()
                        .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                        .collect(Collectors.toList());
                
                // Pagination
                int logsPerPage = 10;
                int maxPage = (int) Math.ceil(logs.size() / (double) logsPerPage);
                
                if (maxPage == 0) {
                    maxPage = 1;
                }
                
                if (page > maxPage) {
                    final int finalMaxPage = maxPage;
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        MessageUtils.sendMessage(sender, "admin.auditlog-page-not-found", Map.of(
                            "page", String.valueOf(page),
                            "max_page", String.valueOf(finalMaxPage)
                        ));
                    });
                    return;
                }
                
                int startIndex = (page - 1) * logsPerPage;
                int endIndex = Math.min(startIndex + logsPerPage, logs.size());
                
                List<AuditLog> pageItems = logs.subList(startIndex, endIndex);
                
                // Send header
                final int finalMaxPage = maxPage;
                final int totalLogs = logs.size();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "admin.auditlog-header", Map.of(
                        "page", String.valueOf(page),
                        "max_page", String.valueOf(finalMaxPage),
                        "total", String.valueOf(totalLogs)
                    ));
                    
                    // Send log entries
                    if (pageItems.isEmpty()) {
                        MessageUtils.sendMessage(sender, "admin.auditlog-no-logs");
                    } else {
                        for (AuditLog log : pageItems) {
                            sendLogEntry(sender, log);
                        }
                    }
                    
                    // Send footer
                    MessageUtils.sendMessage(sender, "admin.auditlog-footer", Map.of(
                        "page", String.valueOf(page),
                        "max_page", String.valueOf(finalMaxPage)
                    ));
                });
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error fetching audit logs: " + e.getMessage());
                e.printStackTrace();
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    MessageUtils.sendMessage(sender, "admin.auditlog-error", Map.of(
                        "error", e.getMessage()
                    ));
                });
            }
        });
        
        return true;
    }
    
    /**
     * Checks if a log entry matches the specified filters.
     *
     * @param log The log entry to check
     * @param filters The filters to apply
     * @return True if the log matches all filters, false otherwise
     */
    private boolean matchesFilters(AuditLog log, Map<String, String> filters) {
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String key = filter.getKey();
            String value = filter.getValue();
            
            if (key.equalsIgnoreCase("action")) {
                if (!log.getActionType().toString().toLowerCase().contains(value.toLowerCase()) &&
                    !log.getActionType().getDisplayName().toLowerCase().contains(value.toLowerCase())) {
                    return false;
                }
            } else if (key.equalsIgnoreCase("target")) {
                // Target is stored in details, filter by substring match
                String details = log.getDetails();
                if (details == null || !details.toLowerCase().contains(value.toLowerCase())) {
                    return false;
                }
            } else if (key.equalsIgnoreCase("executor")) {
                // Filter by actor name
                String actorName = log.getActorName();
                if (actorName == null || !actorName.toLowerCase().contains(value.toLowerCase())) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Process filter arguments.
     *
     * @param args Array of arguments
     * @param startIndex Index to start processing from
     * @param filters Map to store filters
     */
    private void processFilters(String[] args, int startIndex, Map<String, String> filters) {
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            if (arg.contains(":")) {
                String[] parts = arg.split(":", 2);
                if (parts.length == 2) {
                    filters.put(parts[0].toLowerCase(), parts[1]);
                }
            }
        }
    }
    
    /**
     * Sends a formatted log entry to the sender.
     *
     * @param sender Command sender
     * @param log Audit log entry
     */
    private void sendLogEntry(CommandSender sender, AuditLog log) {
        String dateStr = dateFormat.format(new Date(log.getTimestamp()));
        
        MessageUtils.sendMessage(sender, "admin.auditlog-entry", Map.of(
            "id", String.valueOf(log.getId()),
            "date", dateStr,
            "action", log.getActionType().getDisplayName(),
            "target", log.getDetails() != null ? log.getDetails() : "N/A",
            "executor", log.getActorName() != null ? log.getActorName() : "CONSOLE",
            "details", log.getServer() != null ? log.getServer() : ""
        ));
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("1");
            suggestions.add("action:add");
            suggestions.add("action:remove");
            suggestions.add("action:modify");
            suggestions.add("target:");
            suggestions.add("executor:");
            return suggestions;
        } else if (args.length > 1) {
            // Suggest filter types
            String lastArg = args[args.length - 1].toLowerCase();
            if (lastArg.startsWith("action:")) {
                return List.of(
                    "action:add",
                    "action:remove",
                    "action:modify"
                );
            } else if (lastArg.startsWith("target:") || lastArg.startsWith("executor:")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    List<String> players = new ArrayList<>();
                    for (Player p : player.getServer().getOnlinePlayers()) {
                        players.add(lastArg.split(":")[0] + ":" + p.getName());
                    }
                    return players;
                }
            }
        }
        return List.of();
    }
} 