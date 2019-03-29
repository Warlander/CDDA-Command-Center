package pl.warlander.cdda.launcher.model.directories;

import com.google.gson.Gson;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoriesManager {

    private static final Logger logger = LoggerFactory.getLogger(DirectoriesManager.class);
    
    private File rootFolder;
    private File currentVersionFolder;

    private File propertiesFile;
    private LauncherProperties launcherProperties;

    public void initialize() {
        rootFolder = new File("CDDA CC");
        rootFolder.mkdir();

        currentVersionFolder = new File(rootFolder, "Current Version");
        currentVersionFolder.mkdir();

        propertiesFile = new File(rootFolder, "properties.json");
        try {
            propertiesFile.createNewFile();
        } catch (IOException ex) {
            logger.error("Unable to create properties file", ex);
        }

        loadProperties();
        saveProperties();
    }
    
    public File extractAndInstallVersion(String name, File archiveFile) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archiveFile));
            ArchiveInputStream input = new ArchiveStreamFactory().createArchiveInputStream(bis);
            
            logger.info("Extracting " + name);
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
            
            logger.info("Extracted " + name);
            input.close();
            return currentVersionFolder;
        } catch (IOException ex) {
            logger.error("Unable to extract " + name, ex);
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
