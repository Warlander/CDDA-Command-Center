package pl.warlander.cdda.launcher.model.builds;

import java.time.LocalDateTime;
import pl.warlander.cdda.launcher.utils.TimeUtils;

public class BuildData {
    
    private final String name;
    private final String graphics;
    private final String downloadLink;
    private final LocalDateTime timestamp;
    
    public BuildData(String name, String graphics, String downloadLink, LocalDateTime timestamp) {
        this.name = name;
        this.graphics = graphics;
        this.downloadLink = downloadLink;
        this.timestamp = timestamp;
    }
    
    public String getName() {
        return name;
    }
    
    public String getGraphics() {
        return graphics;
    }
    
    public String getDownloadLink() {
        return downloadLink;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String toString() {
        return name + " (" + TimeUtils.timestampToNowString(timestamp) + ")";
    }
    
}
