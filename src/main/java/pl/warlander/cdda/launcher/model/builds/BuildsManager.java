package pl.warlander.cdda.launcher.model.builds;

import java.io.IOException;

public interface BuildsManager {
    
    public abstract BuildData[] fetchBuilds(boolean tiles) throws IOException;
    
    public static BuildsManager createBuildsManager(boolean experimental) {
        if (experimental) {
            return new ExperimentalBuildsManager();
        }
        else {
            return new StableBuildsManager();
        }
    }
    
}
