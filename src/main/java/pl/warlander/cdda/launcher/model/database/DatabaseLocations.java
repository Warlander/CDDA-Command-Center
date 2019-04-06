package pl.warlander.cdda.launcher.model.database;

import pl.warlander.cdda.launcher.model.common.LauncherResource;

public class DatabaseLocations {
    
    private final int apiVersion;
    private final LauncherResource resource;
    private final DatabaseFileLocation[] mods;
    private final DatabaseFileLocation[] soundpacks;
    private final DatabaseFileLocation[] tilesets;
    
    private DatabaseLocations() {
        apiVersion = 1;
        resource = null;
        mods = null;
        soundpacks = null;
        tilesets = null;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    public LauncherResource getResource() {
        return resource;
    }

    public DatabaseFileLocation[] getMods() {
        return mods;
    }

    public DatabaseFileLocation[] getSoundpacks() {
        return soundpacks;
    }

    public DatabaseFileLocation[] getTilesets() {
        return tilesets;
    }
    
}
