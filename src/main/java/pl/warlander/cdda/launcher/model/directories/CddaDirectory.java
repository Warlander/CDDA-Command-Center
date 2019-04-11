package pl.warlander.cdda.launcher.model.directories;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CddaDirectory {
    
    private static final Logger logger = LoggerFactory.getLogger(CddaDirectory.class);
    
    private final File root;
    
    public CddaDirectory(File root) {
        this.root = root;
    }
    
    public GameModInfo[] findMods() {
        File modsDirectory = getModsDirectory();
        
        if (!modsDirectory.exists()) {
            return new GameModInfo[0];
        }
        
        ArrayList<GameModInfo> mods = new ArrayList();
        for (File modDirectory : modsDirectory.listFiles()) {
            File modInfoFile = new File(modDirectory, "modinfo.json");
            if (!modInfoFile.exists()) {
                continue;
            }
            
            Gson gson = new Gson();
            try {
                String modInfoString = FileUtils.readFileToString(modInfoFile, Charset.defaultCharset());
                JsonArray modInfoArray = gson.fromJson(modInfoString, JsonArray.class);
                JsonObject modInfoObject = modInfoArray.get(0).getAsJsonObject();
                String name = modInfoObject.get("name").getAsString();
                JsonElement categoryElement = modInfoObject.get("category");
                String category = categoryElement == null ? "no category" : categoryElement.getAsString();
                String description = modInfoObject.get("description").getAsString();
                mods.add(new GameModInfo(modDirectory, name, category, description));
            } catch (IOException ex) {
                logger.error("Unable to read mod info", ex);
            }
        }
        
        return mods.toArray(GameModInfo[]::new);
    }
    
    public File getExecutable() {
        if (!root.exists()) {
            return null;
        }
        
        File tilesExecutable = new File(root, "cataclysm-tiles.exe");
        if (tilesExecutable.exists()) {
            return tilesExecutable;
        }
        
        File cursesExecutable = new File(root, "cataclysm.exe");
        if (cursesExecutable.exists()) {
            return cursesExecutable;
        }
        
        return null;
    }
    
    public File getModsDirectory() {
        return new File(root, "data/mods");
    }
    
    public File getConfigDirectory() {
        return new File(root, "config");
    }
    
    public File getTemplatesDirectory() {
        return new File(root, "templates");
    }
    
    public File getMemorialDirectory() {
        return new File(root, "memorial");
    }
    
    public File getSavesDirectory() {
        return new File(root, "save");
    }
    
    public File getGraveyardDirectory() {
        return new File(root, "graveyard");
    }
    
    public String getName() {
        return root.getName();
    }
    
    public File getRoot() {
        return root;
    }
    
}
