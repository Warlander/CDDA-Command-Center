package pl.warlander.cdda.launcher.model.builds;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

public class StableBuildsManager implements BuildsManager {

    private static final String BUILDS_PATH = "https://api.github.com/repos/CleverRaven/Cataclysm-DDA/releases";
    
    private static final String OSX_PREFIX = "OSX";
    private static final String WIN_PREFIX = "Win64";
    private static final String LINUX_PREFIX = "Linux";
    
    private static final String TILES_INFIX = "Tiles";
    private static final String CURSES_INFIX = "Curses";
    
    public BuildData[] fetchBuilds(boolean tiles) throws IOException {
        String graphicsInfix = tiles ? TILES_INFIX : CURSES_INFIX;
        String lookupString = getCurrentPlatformPrefix() + "-" + graphicsInfix;
        
        ArrayList<BuildData> builds = new ArrayList();
        
        String buildsJsonString = IOUtils.toString(new URL(BUILDS_PATH), Charset.defaultCharset());
        Gson gson = new Gson();
        JsonArray buildsArray = gson.fromJson(buildsJsonString, JsonArray.class);
        for (JsonElement build : buildsArray) {
            JsonObject buildObject = (JsonObject) build;
            String name = buildObject.get("tag_name").getAsString();
            String timestampString = buildObject.get("published_at").getAsString();
            String download = null;
            JsonArray assetsArray = buildObject.getAsJsonArray("assets");
            for (JsonElement asset : assetsArray) {
                JsonObject assetObject = (JsonObject) asset;
                String assetName = assetObject.get("name").getAsString();
                if (!assetName.contains(lookupString)) {
                    continue;
                }
                download = assetObject.get("browser_download_url").getAsString();
                break;
            }
            if (download == null) {
                continue;
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime timestamp = LocalDateTime.parse(timestampString, formatter);
            builds.add(new BuildData(name, graphicsInfix, download, timestamp));
        }
        
        return builds.toArray(new BuildData[builds.size()]);
    }
    
    private String getCurrentPlatformPrefix() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return WIN_PREFIX;
        }
        else if (SystemUtils.IS_OS_LINUX) {
            return LINUX_PREFIX;
        }
        else if (SystemUtils.IS_OS_MAC) {
            return OSX_PREFIX;
        }
        else {
            throw new IllegalStateException("Unsupported OS. At the moment supported OS are Windows, Linux and OSX.");
        }
    }
    
}
