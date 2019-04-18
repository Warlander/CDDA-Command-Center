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
    
    private final File rootDirectory;
    
    private final File propertiesFile;
    private LauncherProperties launcherProperties;
    private final File modsFile;
    
    private final File gameDirectory;
    
    private final File databaseDirectory;
    private final URL defaultDatabaseLocationsUrl;
    private final File databaseLocationsFile;
    private DatabaseLocations databaseLocations;

    public DirectoriesManager() {
        rootDirectory = new File("CDDA CC");
        gameDirectory = new File(rootDirectory, "Game");
        databaseDirectory = new File(rootDirectory, "Database");
        propertiesFile = new File(rootDirectory, "properties.json");
        modsFile = new File(rootDirectory, "mods.json");
        databaseLocationsFile = new File(databaseDirectory, "databaseLocations.json");
        defaultDatabaseLocationsUrl = getClass().getResource("/database/databaseLocations.json");
    }
    
    public void initialize() {
        rootDirectory.mkdir();
        gameDirectory.mkdir();

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
    
    public CddaDirectory findOldBackupDirectory() {
        File[] files = gameDirectory.listFiles();
        for (File file : files) {
            if (file.isDirectory() && file.getName().endsWith(OLD_BACKUP_STRING)) {
                return new CddaDirectory(file);
            }
        }
        
        return null;
    }
    
    public CddaDirectory findBackupDirectory() {
        File[] files = gameDirectory.listFiles();
        for (File file : files) {
            if (file.isDirectory() && file.getName().endsWith(BACKUP_STRING)) {
                return new CddaDirectory(file);
            }
        }
        
        return null;
    }
    
    public CddaDirectory findCurrentGameDirectory() {
        File[] files = gameDirectory.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory() && !fileName.endsWith(BACKUP_STRING) && !fileName.endsWith(OLD_BACKUP_STRING)) {
                return new CddaDirectory(file);
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
            File databaseFile = new File(databaseDirectory, databaseLocation.getName());
            
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
            File databaseFile = new File(databaseDirectory, databaseLocation.getName());
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
        CddaDirectory backupDirectory = findBackupDirectory();
        if (backupDirectory == null) {
            logger.info("No backup found, aborting restoration");
            return false;
        }
        
        CddaDirectory currentGameDirectory = findCurrentGameDirectory();
        if (currentGameDirectory != null) {
            logger.info("Existing game installation found, deleting");
            try {
                FileUtils.deleteDirectory(currentGameDirectory.getRoot());
            } catch (IOException ex) {
                logger.error("Unable to delete game installation", ex);
                return false;
            }
        }
        File newGameDirectory = new File(gameDirectory, backupDirectory.getName().replace(BACKUP_STRING, "").trim());
        
        try {
            FileUtils.copyDirectory(backupDirectory.getRoot(), newGameDirectory);
        } catch (IOException ex) {
            logger.error("Unable to restore backup", ex);
        }
        
        return true;
    }
    
    public CddaDirectory backupCurrentVersion() {
        logger.info("Starting backup process");
        CddaDirectory currentGameDirectory = findCurrentGameDirectory();
        if (currentGameDirectory == null || !currentGameDirectory.isValid()) {
            logger.info("No game installation found, aborting backup");
            return null;
        }
        
        CddaDirectory backupDirectory = findBackupDirectory();
        if (backupDirectory != null) {
            logger.info("Existing backup found, moving to old backup directory");
            CddaDirectory oldBackupDirectory = findOldBackupDirectory();
            if (oldBackupDirectory != null) {
                logger.warn("Deleting old backup (this shouldn't happen unless launcher was closed mid-backup previously)");
                try {
                    FileUtils.deleteDirectory(oldBackupDirectory.getRoot());
                } catch (IOException ex) {
                    logger.error("Unable to delete old backup", ex);
                    return null;
                }
            }
            File newOldBackupDirectory = new File(gameDirectory, backupDirectory.getName().replace(BACKUP_STRING, OLD_BACKUP_STRING));
            boolean renamed = backupDirectory.getRoot().renameTo(newOldBackupDirectory);
            if (!renamed) {
                logger.error("Unable to move backup to old backup directory");
                return null;
            }
        }
        
        logger.info("Creating backup");
        File newBackupDirectory = new File(gameDirectory, currentGameDirectory.getName() + " " + BACKUP_STRING);
        boolean renamed = currentGameDirectory.getRoot().renameTo(newBackupDirectory);
        if (!renamed) {
            logger.error("Unable to move current game to backup directory");
            return null;
        }
        
        CddaDirectory oldBackupDirectory = findOldBackupDirectory();
        if (oldBackupDirectory != null) {
            logger.info("Deleting old backup");
            try {
                FileUtils.deleteDirectory(oldBackupDirectory.getRoot());
            } catch (IOException ex) {
                logger.error("Unable to delete old backup", ex);
            }
        }
        
        return backupDirectory;
    }
    
    public File extractAndInstallVersion(BuildData data, File archiveFile) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archiveFile));
            ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(bis);
            
            String buildString = data.getName() + " " + data.getGraphics();
            File currentVersionFolder = new File(gameDirectory, buildString);
            
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
