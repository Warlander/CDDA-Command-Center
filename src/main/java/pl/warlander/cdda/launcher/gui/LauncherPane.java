package pl.warlander.cdda.launcher.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.StatusBar;
import pl.warlander.cdda.launcher.model.DirectoriesManager;

public class LauncherPane extends BorderPane {
    
    private final TabPane tabPane;
    private final StatusBar statusBar;
    
    private final GamePane gamePane;
    private final ModsPane modsPane;
    private final SoundpacksPane soundpacksPane;
    private final BackupsPane backupsPane;
    private final SettingsPane settingsPane;
    
    private final ExecutorService executor;
    
    private final DirectoriesManager directoriesManager;
    
    public LauncherPane() {
        setDisable(true);
        executor = Executors.newSingleThreadExecutor();
        statusBar = new StatusBar();
        setBottom(statusBar);
        
        directoriesManager = new DirectoriesManager();
        submitTask(() -> {
            Platform.runLater(() -> statusBar.setText("Initializing file system"));
            directoriesManager.initialize();
            Platform.runLater(() -> {
                setDisable(false);
            });
        });
        
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
    
}
