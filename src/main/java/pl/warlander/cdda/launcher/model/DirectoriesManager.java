package pl.warlander.cdda.launcher.model;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoriesManager {
    
    private File rootFolder;
    private File gameVersionsFolder;
    
    private File configFile;
    
    public void initialize() {
        rootFolder = new File("CDDA CCC");
        rootFolder.mkdirs();
        
        gameVersionsFolder = new File(rootFolder, "Versions");
        gameVersionsFolder.mkdir();
        
        configFile = new File(rootFolder, "config.xml");
        try {
            configFile.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(DirectoriesManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
