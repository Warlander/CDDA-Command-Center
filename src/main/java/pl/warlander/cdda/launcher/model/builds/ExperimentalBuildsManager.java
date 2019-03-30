package pl.warlander.cdda.launcher.model.builds;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.apache.commons.lang3.SystemUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ExperimentalBuildsManager implements BuildsManager {
    
    private static final String BUILDS_PATH = "http://dev.narc.ro/cataclysm/jenkins-latest";
    
    private static final String OSX_FOLDER = "OSX";
    private static final String WIN_FOLDER = "Windows_x64";
    private static final String LINUX_FOLDER = "Linux_x64";
    
    private static final String TILES_FOLDER = "Tiles";
    private static final String CURSES_FOLDER = "Curses";
    
    protected ExperimentalBuildsManager() {}
    
    public BuildData[] fetchBuilds(boolean tiles) throws IOException {
        String graphicsFolder = tiles ? TILES_FOLDER : CURSES_FOLDER;
        String currentPlatformBuilds = BUILDS_PATH + "/" + getCurrentPlatformFolder() + "/" + graphicsFolder;
        
        ArrayList<BuildData> builds = new ArrayList();
        Document doc = Jsoup.connect(currentPlatformBuilds).get();
        
        Elements links = doc.select(("a[href]"));
        for (Element link : links) {
            String name = link.html();
            if (!name.startsWith("cataclysmdda")) {
                continue;
            }
            name = name.substring(name.lastIndexOf("-") + 1);
            name = name.substring(0, name.indexOf("."));
            String downloadLink = link.attr(("abs:href"));
            
            String dateAndSize = link.nextSibling().outerHtml();
            String[] dateAndSizeParts = dateAndSize.trim().split("\\s+");
            String date = dateAndSizeParts[0] + " " + dateAndSizeParts[1];
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime timestamp = LocalDateTime.parse(date, formatter);
            
            BuildData buildData = new BuildData(name, graphicsFolder, downloadLink, timestamp);
            builds.add(buildData);
        }
        
        return builds.toArray(new BuildData[builds.size()]);
    }
    
    private String getCurrentPlatformFolder() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return WIN_FOLDER;
        }
        else if (SystemUtils.IS_OS_LINUX) {
            return LINUX_FOLDER;
        }
        else if (SystemUtils.IS_OS_MAC) {
            return OSX_FOLDER;
        }
        else {
            throw new IllegalStateException("Unsupported OS. At the moment supported OS are Windows, Linux and OSX.");
        }
    }
    
}
