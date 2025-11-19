package com.mcmodsync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcmodsync.McModSync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModSyncConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = Paths.get("config", "mcmodsync.json");
    
    private static ModSyncConfig instance;
    
    private String serverAddress = "http://localhost:56552";
    private boolean autoSync = false;
    
    private ModSyncConfig() {
        // Private constructor for singleton
    }
    
    public static ModSyncConfig getInstance() {
        if (instance == null) {
            instance = new ModSyncConfig();
            instance.load();
        }
        return instance;
    }
    
    /**
     * Load configuration from file
     */
    public void load() {
        if (!Files.exists(CONFIG_FILE)) {
            McModSync.LOGGER.info("Config file not found, using defaults");
            return;
        }
        
        try {
            String json = Files.readString(CONFIG_FILE);
            ModSyncConfig loaded = GSON.fromJson(json, ModSyncConfig.class);
            if (loaded != null) {
                this.serverAddress = loaded.serverAddress;
                this.autoSync = loaded.autoSync;
                McModSync.LOGGER.info("Loaded config: serverAddress={}, autoSync={}", 
                    serverAddress, autoSync);
            }
        } catch (IOException e) {
            McModSync.LOGGER.error("Failed to load config file", e);
        }
    }
    
    /**
     * Save configuration to file
     */
    public void save() {
        try {
            // Ensure config directory exists
            Path configDir = CONFIG_FILE.getParent();
            if (configDir != null && !Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_FILE, json);
            McModSync.LOGGER.info("Saved config: serverAddress={}, autoSync={}", 
                serverAddress, autoSync);
        } catch (IOException e) {
            McModSync.LOGGER.error("Failed to save config file", e);
            throw new RuntimeException("Failed to save config", e);
        }
    }
    
    // Getters and setters
    public String getServerAddress() {
        return serverAddress;
    }
    
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }
    
    public boolean isAutoSync() {
        return autoSync;
    }
    
    public void setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
    }
}
