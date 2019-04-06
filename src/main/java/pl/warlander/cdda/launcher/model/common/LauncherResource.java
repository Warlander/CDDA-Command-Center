package pl.warlander.cdda.launcher.model.common;

public class LauncherResource {
    
    private final String location;
    private final LauncherResourceType type;
    
    public LauncherResource(String location, LauncherResourceType type) {
        this.location = location;
        this.type = type;
    }
    
    public String getLocation() {
        return location;
    }
    
    public LauncherResourceType getType() {
        return type;
    }
    
}
