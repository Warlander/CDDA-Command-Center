package pl.warlander.cdda.launcher.model.changelog;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pl.warlander.cdda.launcher.utils.TimeUtils;

public class ExperimentalChangelogManager implements ChangelogManager {
    
    private static final String EXPERIMENTAL_BUILDS_CHANGELOG = "http://gorgon.narc.ro:8080/job/Cataclysm-Matrix/api/xml?tree=builds[number,timestamp,changeSet[items[msg]]]{0,45}";
    
    private Document changelogDocument;
    
    protected ExperimentalChangelogManager() {}
    
    public void downloadChangelog() throws IOException {
        changelogDocument = Jsoup.connect(EXPERIMENTAL_BUILDS_CHANGELOG).get();
    }
    
    public String ParseChangelog() {
        if (changelogDocument == null) {
            throw new IllegalStateException("Attempted parsing non-downloaded changelog");
        }
        
        StringBuilder changelog = new StringBuilder();
        Elements jenkinsBuilds = changelogDocument.getElementsByTag("build");
        for (Element jenkinsBuild : jenkinsBuilds) {
            String buildNumber = jenkinsBuild.getElementsByTag("number").html();
            long buildTimestamp = Long.parseLong(jenkinsBuild.getElementsByTag("timestamp").html()) / 1000;
            LocalDateTime time = LocalDateTime.ofEpochSecond(buildTimestamp, 0, ZoneOffset.UTC);
            String timeToNowString = TimeUtils.timestampToNowString(time);

            changelog.append("<p><b>").append(buildNumber).append(" (").append(timeToNowString).append(" - ").append(time.toString().replace("T", " ")).append(")</b></p>");

            changelog.append("<ul>");
            Elements buildChangelogLines = jenkinsBuild.getElementsByTag("msg");
            for (Element buildChangelogLine : buildChangelogLines) {
                changelog.append("<li>").append(buildChangelogLine.html()).append("</li>");
            }
            changelog.append("</ul>");
        }
        
        return changelog.toString();
    }
    
}
