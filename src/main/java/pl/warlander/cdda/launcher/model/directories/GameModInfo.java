package pl.warlander.cdda.launcher.model.directories;

import java.io.File;

public class GameModInfo {
    
    private final File folder;
    private final String name;
    private final String category;
    private final String description;
    
    public GameModInfo(File folder, String name, String category, String description) {
        this.folder = folder;
        this.name = name;
        this.category = category;
        this.description = description;
    }
    
    public File getFolder() {
        return folder;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getDescription() {
        return description;
    }
    
}
