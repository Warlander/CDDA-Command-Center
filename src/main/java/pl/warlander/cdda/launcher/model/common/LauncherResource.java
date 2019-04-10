package pl.warlander.cdda.launcher.model.common;

import java.net.MalformedURLException;
import java.net.URL;

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
    
    public URL getAsURL() {
        switch (type) {
            case JAR:
                return getClass().getResource(location);
            case DOWNLOAD:
            {
                try {
                    return new URL(location);
                } catch (MalformedURLException ex) {
                    return null;
                }
            }
            case GITHUB:
                String finalLocation = location.replaceFirst("github", "raw.githubusercontent").replaceFirst("/blob", "");
                try {
                    return new URL(finalLocation);
                } catch (MalformedURLException ex) {
                    return null;
                }
            default:
                return null;
        }
        
    }
    
}
