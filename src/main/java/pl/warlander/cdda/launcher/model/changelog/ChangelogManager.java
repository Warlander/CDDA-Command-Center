package pl.warlander.cdda.launcher.model.changelog;

import java.io.IOException;

public interface ChangelogManager {
    
    public abstract void downloadChangelog() throws IOException;
    public abstract String ParseChangelog();
    
    public static ChangelogManager createChangelogManager(boolean experimental) {
        if (experimental) {
            return new ExperimentalChangelogManager();
        }
        else {
            return new StableChangelogManager();
        }
    }
    
}
