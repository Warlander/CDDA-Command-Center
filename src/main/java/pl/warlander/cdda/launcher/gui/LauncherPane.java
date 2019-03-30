package pl.warlander.cdda.launcher.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.StatusBar;
import org.kamranzafar.jddl.DirectDownloader;
import org.kamranzafar.jddl.DownloadAdaptor;
import org.kamranzafar.jddl.DownloadTask;
import pl.warlander.cdda.launcher.model.directories.DirectoriesManager;

public class LauncherPane extends BorderPane {
    
    private final TabPane tabPane;
    private final StatusBar statusBar;
    
    private final GamePane gamePane;
    private final ModsPane modsPane;
    private final SoundpacksPane soundpacksPane;
    private final BackupsPane backupsPane;
    private final SettingsPane settingsPane;
    
    private final ExecutorService executor;
    
    private final DirectDownloader downloader;
    private final DirectoriesManager directoriesManager;
    
    public LauncherPane() {
        executor = Executors.newSingleThreadExecutor();
        statusBar = new StatusBar();
        setBottom(statusBar);
        
        downloader = new DirectDownloader();
        directoriesManager = new DirectoriesManager();
        directoriesManager.initialize();
        
        gamePane = new GamePane(this);
        Tab game = createTab("Game", gamePane);
        modsPane = new ModsPane();
        Tab mods = createTab("Mods", modsPane);
        soundpacksPane = new SoundpacksPane();
        Tab soundpacks = createTab("Soundpacks", soundpacksPane);
        backupsPane = new BackupsPane();
        Tab backups = createTab("Backups", backupsPane);
        settingsPane = new SettingsPane();
        Tab settings = createTab("Settings", settingsPane);
        
        tabPane = new TabPane(game, mods, soundpacks, backups, settings);
        setCenter(tabPane);
    }
    
    private Tab createTab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }
    
    public void submitDownload(DownloadTask downloadTask) {
        downloader.download(downloadTask);
        submitTask(downloader);
        AtomicBoolean downloading = new AtomicBoolean(true);
        downloadTask.addListener(new DownloadAdaptor() {
            public void onComplete() {
                downloading.set(false);
            }

            public void onCancel() {
                downloading.set(false);
            }
        });
        submitTask(() -> {
            while (downloading.get()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {}
            }
        });
    }
    
    public void submitTask(Runnable runnable) {
        executor.submit(() -> {
            Platform.runLater(() -> statusBar.setText("Starting new task"));
            runnable.run();
            Platform.runLater(() -> statusBar.setText("Ready"));
        });
    }
    
    public StatusBar getStatusBar() {
        return statusBar;
    }
    
    public DirectoriesManager getDirectoriesManager() {
        return directoriesManager;
    }
    
}
