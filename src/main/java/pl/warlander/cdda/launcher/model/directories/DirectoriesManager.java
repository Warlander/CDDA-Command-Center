package pl.warlander.cdda.launcher.model.directories;

import com.google.gson.Gson;
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

public class DirectoriesManager {

    private static final Logger logger = LoggerFactory.getLogger(DirectoriesManager.class);
    
    private static final String OLD_BACKUP_STRING = "BackupOld";
    private static final String BACKUP_STRING = "Backup";
    
    private final File rootFolder;
    private final File gameFolder;

    private final File propertiesFile;
    private LauncherProperties launcherProperties;

    public DirectoriesManager() {
        rootFolder = new File("CDDA CC");
        gameFolder = new File(rootFolder, "Game");
        propertiesFile = new File(rootFolder, "properties.json");
    }
    
    public void initialize() {
        rootFolder.mkdir();
        gameFolder.mkdir();

        try {
            propertiesFile.createNewFile();
        } catch (IOException ex) {
            logger.error("Unable to create properties file", ex);
        }

        loadProperties();
        saveProperties();
    }
    
    public ModInfo[] findMods(File gameRootDirectory) {
        File modsDirectory = new File(gameRootDirectory, "data/mods");
        
        if (!modsDirectory.exists()) {
            return new ModInfo[0];
        }
        
        ArrayList<ModInfo> mods = new ArrayList();
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
                mods.add(new ModInfo(modDirectory, name, category, description));
            } catch (IOException ex) {
                logger.error("Unable to read mod info", ex);
            }
        }
        
        return mods.toArray(ModInfo[]::new);
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
        Gson gson = new Gson();
        try {
            FileWriter writer = new FileWriter(propertiesFile);
            writer.write(gson.toJson(launcherProperties));
            writer.close();
        } catch (IOException ex) {
            logger.error("Unable to save properties", ex);
        }
    }

    public LauncherProperties getLauncherProperties() {
        return launcherProperties;
    }

}
