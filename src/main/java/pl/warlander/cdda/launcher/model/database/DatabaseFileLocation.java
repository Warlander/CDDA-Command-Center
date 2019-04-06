package pl.warlander.cdda.launcher.model.database;

import pl.warlander.cdda.launcher.model.common.LauncherResource;

public class DatabaseFileLocation {
    
    private final String name;
    private final LauncherResource resource;
    private final LauncherResource defaultResource;
    
    private DatabaseFileLocation() {
        name = null;
        resource = null;
        defaultResource = null;
    }
    
    public String getName() {
        return name;
    }
    
    public LauncherResource getResource() {
        return resource;
    }
    
    public LauncherResource getDefaultResource() {
        return defaultResource;
    }
    
}
