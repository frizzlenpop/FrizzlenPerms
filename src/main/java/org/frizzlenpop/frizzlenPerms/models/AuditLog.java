package org.frizzlenpop.frizzlenPerms.models;

import java.util.UUID;

/**
 * Represents an audit log entry for tracking actions in the plugin.
 */
public class AuditLog {
    
    private final UUID id;
    private final UUID actorUuid;
    private final String actorName;
    private final long timestamp;
    private final ActionType actionType;
    private final String details;
    private final String server;
    private final UUID targetPlayerId;
    private final String targetDiscordId;
    
    /**
     * Creates a new audit log entry.
     *
     * @param id The unique ID of the log entry
     * @param actorUuid The UUID of the actor who performed the action
     * @param actorName The name of the actor who performed the action
     * @param timestamp The timestamp of the action
     * @param actionType The type of action
     * @param details The details of the action
     * @param server The server where the action was performed
     */
    public AuditLog(UUID id, UUID actorUuid, String actorName, long timestamp, ActionType actionType, String details, String server) {
        this(id, actorUuid, actorName, timestamp, actionType, details, server, null, null);
    }

    /**
     * Creates a new audit log entry with a target player ID.
     *
     * @param id The unique ID of the log entry
     * @param actorUuid The UUID of the actor who performed the action
     * @param actorName The name of the actor who performed the action
     * @param timestamp The timestamp of the action
     * @param actionType The type of action
     * @param details The details of the action
     * @param server The server where the action was performed
     * @param targetPlayerId The UUID of the target player
     */
    public AuditLog(UUID id, UUID actorUuid, String actorName, long timestamp, ActionType actionType, String details, String server, UUID targetPlayerId) {
        this(id, actorUuid, actorName, timestamp, actionType, details, server, targetPlayerId, null);
    }

    /**
     * Creates a new audit log entry with a target Discord ID.
     *
     * @param id The unique ID of the log entry
     * @param actorUuid The UUID of the actor who performed the action
     * @param actorName The name of the actor who performed the action
     * @param timestamp The timestamp of the action
     * @param actionType The type of action
     * @param details The details of the action
     * @param server The server where the action was performed
     * @param targetDiscordId The Discord ID of the target
     */
    public AuditLog(UUID id, UUID actorUuid, String actorName, long timestamp, ActionType actionType, String details, String server, String targetDiscordId) {
        this(id, actorUuid, actorName, timestamp, actionType, details, server, null, targetDiscordId);
    }

    /**
     * Creates a new audit log entry with both target player ID and Discord ID.
     *
     * @param id The unique ID of the log entry
     * @param actorUuid The UUID of the actor who performed the action
     * @param actorName The name of the actor who performed the action
     * @param timestamp The timestamp of the action
     * @param actionType The type of action
     * @param details The details of the action
     * @param server The server where the action was performed
     * @param targetPlayerId The UUID of the target player
     * @param targetDiscordId The Discord ID of the target
     */
    public AuditLog(UUID id, UUID actorUuid, String actorName, long timestamp, ActionType actionType, String details, String server, UUID targetPlayerId, String targetDiscordId) {
        this.id = id;
        this.actorUuid = actorUuid;
        this.actorName = actorName;
        this.timestamp = timestamp;
        this.actionType = actionType;
        this.details = details;
        this.server = server;
        this.targetPlayerId = targetPlayerId;
        this.targetDiscordId = targetDiscordId;
    }
    
    /**
     * Creates a new audit log entry with a string ID.
     *
     * @param id The string ID of the log entry
     * @param actorUuid The UUID of the actor who performed the action
     * @param actorName The name of the actor who performed the action
     * @param timestamp The timestamp of the action
     * @param actionType The type of action
     */
    public AuditLog(long id, String actorName, UUID actorUuid, UUID targetUuid, String actionData) {
        this(UUID.randomUUID(), actorUuid, actorName, System.currentTimeMillis(), ActionType.fromDisplayName(actionData), actionData, "default", targetUuid);
    }
    
    /**
     * Gets the unique ID of the log entry.
     *
     * @return The unique ID of the log entry
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Gets the UUID of the actor who performed the action.
     *
     * @return The UUID of the actor
     */
    public UUID getActorUuid() {
        return actorUuid;
    }
    
    /**
     * Gets the name of the actor who performed the action.
     *
     * @return The name of the actor
     */
    public String getActorName() {
        return actorName;
    }
    
    /**
     * Gets the timestamp of the action.
     *
     * @return The timestamp of the action
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the type of action.
     *
     * @return The type of action
     */
    public ActionType getActionType() {
        return actionType;
    }
    
    /**
     * Gets the details of the action.
     *
     * @return The details of the action
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * Gets the server where the action was performed.
     *
     * @return The server where the action was performed
     */
    public String getServer() {
        return server;
    }
    
    /**
     * Gets the target player ID.
     *
     * @return The target player ID
     */
    public UUID getTargetPlayerId() {
        return targetPlayerId;
    }
    
    /**
     * Gets the target Discord ID.
     *
     * @return The target Discord ID
     */
    public String getTargetDiscordId() {
        return targetDiscordId;
    }
    
    /**
     * Gets the type of the action.
     *
     * @return The type of the action
     */
    public ActionType getType() {
        return actionType;
    }
    
    /**
     * Gets the target UUID.
     *
     * @return The target UUID
     */
    public UUID getTargetUuid() {
        return targetPlayerId;
    }
    
    /**
     * Gets the action data.
     *
     * @return The action data
     */
    public String getActionData() {
        return details;
    }
    
    /**
     * Formats the audit log entry as a string.
     *
     * @return The formatted audit log entry
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(actionType.getDisplayName()).append("] ");
        sb.append("Actor: ").append(actorName);
        if (targetPlayerId != null) {
            sb.append(", Target: ").append(targetPlayerId);
        }
        if (targetDiscordId != null) {
            sb.append(", Discord: ").append(targetDiscordId);
        }
        sb.append(", Details: ").append(details);
        sb.append(", Server: ").append(server);
        return sb.toString();
    }
    
    /**
     * Represents the type of action that was performed.
     */
    public enum ActionType {
        RANK_CREATE("Rank Created"),
        RANK_DELETE("Rank Deleted"),
        RANK_MODIFY("Rank Modified"),
        RANK_SET("Rank Set"),
        RANK_ADD("Rank Added"),
        RANK_REMOVE("Rank Removed"),
        PERMISSION_ADD("Permission Added"),
        PERMISSION_REMOVE("Permission Removed"),
        PERMISSION_SET("Permission Set"),
        DISCORD_LINK("Discord Linked"),
        DISCORD_UNLINK("Discord Unlinked"),
        CONFIG_CHANGE("Config Changed"),
        PLUGIN_RELOAD("Plugin Reloaded"),
        PLAYER_IMPORT("Player Imported"),
        PLAYER_EXPORT("Player Exported"),
        SYNC_PUSH("Sync Pushed"),
        SYNC_PULL("Sync Pulled"),
        PLAYER_RANK_ADD("Player Rank Added"),
        PLAYER_RANK_REMOVE("Player Rank Removed"),
        PLAYER_TEMP_RANK_ADD("Temporary Rank Added"),
        PLAYER_TEMP_RANK_REMOVE("Temporary Rank Removed"),
        PLAYER_RANK_CLONE("Player Rank Cloned"),
        PLAYER_DATA_PURGE("Player Data Purged");
        
        private final String displayName;
        
        ActionType(String displayName) {
            this.displayName = displayName;
        }
        
        /**
         * Gets the display name of the action type.
         *
         * @return The display name of the action type
         */
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * Gets the action type from its display name.
         *
         * @param displayName The display name of the action type
         * @return The action type, or null if not found
         */
        public static ActionType fromDisplayName(String displayName) {
            for (ActionType type : values()) {
                if (type.displayName.equalsIgnoreCase(displayName)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("No action type with display name: " + displayName);
        }
    }
} 