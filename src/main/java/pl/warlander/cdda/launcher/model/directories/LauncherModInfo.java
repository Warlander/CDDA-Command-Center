package pl.warlander.cdda.launcher.model.directories;

import pl.warlander.cdda.launcher.model.mods.ModType;

public class LauncherModInfo {
    
    private final String folderName;
    private final ModType type;
    
    public LauncherModInfo(String folderName, ModType type) {
        this.folderName = folderName;
        this.type = type;
    }
    
    public String getFolderName() {
        return folderName;
    }
    
    public ModType getModType() {
        return type;
    }
    
}
