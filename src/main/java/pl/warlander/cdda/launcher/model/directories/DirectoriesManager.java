package pl.warlander.cdda.launcher.model.directories;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoriesManager {

    private File rootFolder;
    private File gameVersionsFolder;

    private File propertiesFile;
    private LauncherProperties launcherProperties;

    public void initialize() {
        rootFolder = new File("CDDA CCC");
        rootFolder.mkdirs();

        gameVersionsFolder = new File(rootFolder, "Versions");
        gameVersionsFolder.mkdir();

        propertiesFile = new File(rootFolder, "properties.json");
        try {
            propertiesFile.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(DirectoriesManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        loadProperties();
        saveProperties();
    }

    public LauncherProperties loadProperties() {
        Gson gson = new Gson();
        try {
            FileReader reader = new FileReader(propertiesFile);
            launcherProperties = gson.fromJson(reader, LauncherProperties.class);
            reader.close();
        } catch (IOException ex) {
            Logger.getLogger(DirectoriesManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (launcherProperties == null) {
            launcherProperties = new LauncherProperties();
        }
        return launcherProperties;
    }

    public void saveProperties() {
        Gson gson = new Gson();
        try {
            FileWriter writer = new FileWriter(propertiesFile);
            writer.write(gson.toJson(launcherProperties));
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(DirectoriesManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public LauncherProperties getLauncherProperties() {
        return launcherProperties;
    }

}
