package pl.warlander.cdda.launcher.model.directories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.warlander.cdda.launcher.model.builds.BuildData;
import pl.warlander.cdda.launcher.model.database.DatabaseFileLocation;
import pl.warlander.cdda.launcher.model.database.DatabaseLocations;

public class DirectoriesManager {

    private static final Logger logger = LoggerFactory.getLogger(DirectoriesManager.class);
    
    private static final String OLD_BACKUP_STRING = "BackupOld";
    private static final String BACKUP_STRING = "Backup";
    
    private final File rootFolder;
    private final File gameFolder;
    private final File databaseFolder;

    private final File propertiesFile;
    private LauncherProperties launcherProperties;
    
    private final File modsFile;
    
    private final URL defaultDatabaseLocationsUrl;
    private final File databaseLocationsFile;
    private DatabaseLocations databaseLocations;

    public DirectoriesManager() {
        rootFolder = new File("CDDA CC");
        gameFolder = new File(rootFolder, "Game");
        databaseFolder = new File(rootFolder, "Database");
        propertiesFile = new File(rootFolder, "properties.json");
        modsFile = new File(rootFolder, "mods.json");
        databaseLocationsFile = new File(databaseFolder, "databaseLocations.json");
        defaultDatabaseLocationsUrl = getClass().getResource("/database/databaseLocations.json");
    }
    
    public void initialize() {
        rootFolder.mkdir();
        gameFolder.mkdir();

        try {
            propertiesFile.createNewFile();
        } catch (IOException ex) {
            logger.error("Unable to create properties file", ex);
        }
        
        try {
            modsFile.createNewFile();
        } catch (IOException ex) {
            logger.error("Unable to create mods file", ex);
        }

        loadProperties();
        saveProperties();
        
        reloadDatabase();
    }
    
    public File findConfigFolder(File gameRootDirectory) {
        return new File(gameRootDirectory, "config");
    }
    
    public File findTemplatesFolder(File gameRootDirectory) {
        return new File(gameRootDirectory, "templates");
    }
    
    public File findMemorialFolder(File gameRootDirectory) {
        return new File(gameRootDirectory, "memorial");
    }
    
    public File findSavesFolder(File gameRootDirectory) {
        return new File(gameRootDirectory, "save");
    }
    
    public File findGraveyardFolder(File gameRootDirectory) {
        return new File(gameRootDirectory, "graveyard");
    }
    
    public GameModInfo[] findMods(File gameRootDirectory) {
        File modsDirectory = new File(gameRootDirectory, "data/mods");
        
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
    
    public File findCurrentGameExecutable() {
        File currentGameFolder = findCurrentGameFolder();
        if (currentGameFolder == null) {
            return null;
        }
        
        File tilesExecutable = new File(currentGameFolder, "cataclysm-tiles.exe");
        if (tilesExecutable.exists()) {
            return tilesExecutable;
        }
        
        File cursesExecutable = new File(currentGameFolder, "cataclysm.exe");
        if (cursesExecutable.exists()) {
            return cursesExecutable;
        }
        
        return null;
    }
    
    public File findOldBackupFolder() {
        File[] files = gameFolder.listFiles();
        for (File file : files) {
            if (file.isDirectory() && file.getName().endsWith(OLD_BACKUP_STRING)) {
                return file;
            }
        }
        
        return null;
    }
    
    public File findBackupFolder() {
        File[] files = gameFolder.listFiles();
        for (File file : files) {
            if (file.isDirectory() && file.getName().endsWith(BACKUP_STRING)) {
                return file;
            }
        }
        
        return null;
    }
    
    public File findCurrentGameFolder() {
        File[] files = gameFolder.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory() && !fileName.endsWith(BACKUP_STRING) && !fileName.endsWith(OLD_BACKUP_STRING)) {
                return file;
            }
        }
        
        return null;
    }
    
    public void updateDatabase() {
        URL resourceUrl = databaseLocations.getResource().getAsURL();
        if (resourceUrl == null) {
            logger.warn("Unable to update database due to empty resource URL");
            return;
        }
        
        try {
            FileUtils.copyURLToFile(resourceUrl, databaseLocationsFile, 1000, 1000);
            reloadDatabase();
            
            updateDatabases(databaseLocations.getMods());
            updateDatabases(databaseLocations.getSoundpacks());
            updateDatabases(databaseLocations.getTilesets());
        } catch (IOException ex) {
            logger.error("Unable to download updated database", ex);
            return;
        }
    }
    
    private void updateDatabases(DatabaseFileLocation[] databaseLocations) {
        for (DatabaseFileLocation databaseLocation : databaseLocations) {
            File databaseFile = new File(databaseFolder, databaseLocation.getName());
            
            try {
                FileUtils.copyURLToFile(databaseLocation.getResource().getAsURL(), databaseFile);
            } catch (IOException ex) {
                logger.error("Unable to copy " + databaseLocation.getName(), ex);
            }
        }
    }
    
    private void reloadDatabase() {
        if (!databaseLocationsFile.exists()) {
            try {
                FileUtils.copyURLToFile(defaultDatabaseLocationsUrl, databaseLocationsFile);
            } catch (IOException ex) {
                logger.error("Unable to copy the default database to target file", ex);
            }
        }
        
        try (FileReader reader = new FileReader(databaseLocationsFile)) {
            Gson gson = new Gson();
            databaseLocations = gson.fromJson(reader, DatabaseLocations.class);
        } catch (IOException ex) {
            logger.error("Unable to read database locations file", ex);
        }
        
        reloadDatabases(databaseLocations.getMods());
        reloadDatabases(databaseLocations.getSoundpacks());
        reloadDatabases(databaseLocations.getTilesets());
    }
    
    private void reloadDatabases(DatabaseFileLocation[] databaseLocations) {
        for (DatabaseFileLocation databaseLocation : databaseLocations) {
            File databaseFile = new File(databaseFolder, databaseLocation.getName());
            if (databaseFile.exists()) {
                continue;
            }
            
            try {
                FileUtils.copyURLToFile(databaseLocation.getDefaultResource().getAsURL(), databaseFile);
            } catch (IOException ex) {
                logger.error("Unable to copy " + databaseLocation.getName(), ex);
            }
        }
    }
    
    public boolean restoreBackup() {
        logger.info("Starting restoration process");
        File backupFolder = findBackupFolder();
        if (backupFolder == null) {
            logger.info("No backup found, aborting restoration");
            return false;
        }
        
        File currentGameFolder = findCurrentGameFolder();
        if (currentGameFolder != null) {
            logger.info("Existing game installation found, deleting");
            try {
                FileUtils.deleteDirectory(currentGameFolder);
            } catch (IOException ex) {
                logger.error("Unable to delete game installation", ex);
                return false;
            }
        }
        currentGameFolder = new File(backupFolder.getParentFile(), backupFolder.getName().replace(BACKUP_STRING, "").trim());
        
        try {
            FileUtils.copyDirectory(backupFolder, currentGameFolder);
        } catch (IOException ex) {
            logger.error("Unable to restore backup", ex);
        }
        
        return true;
    }
    
    public File backupCurrentVersion() {
        logger.info("Starting backup process");
        File currentGameFolder = findCurrentGameFolder();
        if (currentGameFolder == null) {
            logger.info("No game installation found, aborting backup");
            return null;
        }
        
        File backupFolder = findBackupFolder();
        if (backupFolder != null) {
            logger.info("Existing backup found, moving to old backup directory");
            File oldBackupFolder = findOldBackupFolder();
            if (oldBackupFolder != null) {
                logger.info("Deleting old backup (this shouldn't happen unless launcher was closed mid-backup previously)");
                try {
                    FileUtils.deleteDirectory(oldBackupFolder);
                } catch (IOException ex) {
                    logger.error("Unable to delete old backup", ex);
                    return null;
                }
            }
            oldBackupFolder = new File(backupFolder.getParentFile(), backupFolder.getName().replace(BACKUP_STRING, OLD_BACKUP_STRING));
            boolean renamed = backupFolder.renameTo(oldBackupFolder);
            if (!renamed) {
                logger.error("Unable to move backup to old backup directory");
                return null;
            }
        }
        
        logger.info("Creating backup");
        backupFolder = new File(currentGameFolder.getParentFile(), currentGameFolder.getName() + " " + BACKUP_STRING);
        boolean renamed = currentGameFolder.renameTo(backupFolder);
        if (!renamed) {
            logger.error("Unable to move current game to backup directory");
            return null;
        }
        
        File oldBackupFolder = findOldBackupFolder();
        if (oldBackupFolder != null) {
            logger.info("Deleting old backup");
            try {
                FileUtils.deleteDirectory(oldBackupFolder);
            } catch (IOException ex) {
                logger.error("Unable to delete old backup", ex);
            }
        }
        
        return backupFolder;
    }
    
    public File extractAndInstallVersion(BuildData data, File archiveFile) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archiveFile));
            ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(bis);
            
            String buildString = data.getName() + " " + data.getGraphics();
            File currentVersionFolder = new File(gameFolder, buildString);
            
            logger.info("Extracting " + data.getName());
            ArchiveEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entry.isDirectory()){
                    continue;
                }
                File curfile = new File(currentVersionFolder, entry.getName());
                File parent = curfile.getParentFile();
                if (!parent.exists()) {
                    if (!parent.mkdirs()){
                        logger.info("Could not create directory: " + parent.getPath());
                        throw new RuntimeException("Could not create directory: " + parent.getPath());
                    }
                }
                IOUtils.copy(input, new FileOutputStream(curfile));
            }
            
            logger.info("Extracted " + data.getName());
            input.close();
            return currentVersionFolder;
        } catch (IOException ex) {
            logger.error("Unable to extract " + data.getName(), ex);
            return null;
        } catch (ArchiveException ex) {
            logger.error("Unable to determine compression used in downloaded archive", ex);
            return null;
        }
    }

    public LauncherProperties loadProperties() {
        Gson gson = new Gson();
        try {
            FileReader reader = new FileReader(propertiesFile);
            launcherProperties = gson.fromJson(reader, LauncherProperties.class);
            reader.close();
        } catch (IOException ex) {
            logger.error("Unable to load properties", ex);
        }
        if (launcherProperties == null) {
            launcherProperties = new LauncherProperties();
        }
        return launcherProperties;
    }

    public void saveProperties() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileWriter writer = new FileWriter(propertiesFile);
            writer.write(gson.toJson(launcherProperties));
            writer.close();
        } catch (IOException ex) {
            logger.error("Unable to save properties", ex);
        }
    }
    
    public LauncherModInfo[] loadLauncherModsInfo() {
        Gson gson = new Gson();
        try {
            FileReader reader = new FileReader(modsFile);
            LauncherModInfo[] modsInfo = gson.fromJson(reader, LauncherModInfo[].class);
            reader.close();
            if (modsInfo == null) {
                return new LauncherModInfo[0];
            }
            return modsInfo;
        } catch (IOException ex) {
            logger.error("Unable to load launcher mods info", ex);
            return new LauncherModInfo[0];
        }
    }
    
    public void saveLauncherModsInfo(LauncherModInfo[] modsInfo) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileWriter writer = new FileWriter(modsFile);
            writer.write(gson.toJson(modsInfo));
            writer.close();
        } catch (IOException ex) {
            logger.error("Unable to save launcher mods info", ex);
        }
    }
    
    public LauncherProperties getLauncherProperties() {
        return launcherProperties;
    }

}
