package pl.warlander.cdda.launcher.model.changelog;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.io.IOUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import pl.warlander.cdda.launcher.utils.TimeUtils;

public class StableChangelogManager implements ChangelogManager {

    private static final String CHANGELOGS_PATH = "https://api.github.com/repos/CleverRaven/Cataclysm-DDA/releases";
    private JsonArray changelogsArray;
    
    public void downloadChangelog() throws IOException {
        String buildsJsonString = IOUtils.toString(new URL(CHANGELOGS_PATH), Charset.defaultCharset());
        Gson gson = new Gson();
        changelogsArray = gson.fromJson(buildsJsonString, JsonArray.class);
    }

    public String ParseChangelog() {
        StringBuilder changelog = new StringBuilder();
        
        for (JsonElement build : changelogsArray) {
            JsonObject buildObject = (JsonObject) build;
            String name = buildObject.get("tag_name").getAsString();
            String body = buildObject.get("body").getAsString();
            String timestampString = buildObject.get("published_at").getAsString();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime timestamp = LocalDateTime.parse(timestampString, formatter);
            String timeToNowString = TimeUtils.timestampToNowString(timestamp);
            
            changelog.append("<p><b>").append(name).append(" (").append(timeToNowString).append(" - ").append(timestamp.toString().replace("T", " ")).append(")</b></p>");
            Parser parser = Parser.builder().build();
            Node document = parser.parse(body);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String renderedMarkdown = renderer.render(document);
            changelog.append(renderedMarkdown);
        }
        
        return changelog.toString();
    }
    
}
