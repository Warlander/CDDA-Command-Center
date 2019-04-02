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
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;
import org.kamranzafar.jddl.DirectDownloader;
import org.kamranzafar.jddl.DownloadAdaptor;
import org.kamranzafar.jddl.DownloadTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.warlander.cdda.launcher.model.directories.DirectoriesManager;

public class LauncherPane extends BorderPane {
    
    private static final Logger logger = LoggerFactory.getLogger(LauncherPane.class);
    
    private final TabPane tabPane;
    private final StatusBar statusBar;
    
    private final GamePane gamePane;
    private final ModsPane modsPane;
    private final TilesetsPane tilesetsPane;
    private final SoundpacksPane soundpacksPane;
    private final WorldsPane worldsPane;
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
        
        GlyphFont fontAwesome = GlyphFontRegistry.font("FontAwesome");
        
        gamePane = new GamePane(this);
        Glyph gamePaneGlyph = fontAwesome.create(FontAwesome.Glyph.GAMEPAD);
        Tab game = createTab("Game", gamePaneGlyph, gamePane);
        modsPane = new ModsPane(this);
        Glyph modsPaneGlyph = fontAwesome.create(FontAwesome.Glyph.PUZZLE_PIECE);
        Tab mods = createTab("Mods", modsPaneGlyph, modsPane);
        tilesetsPane = new TilesetsPane();
        Glyph tilesetsPaneGlyph = fontAwesome.create(FontAwesome.Glyph.PHOTO);
        Tab tilesets = createTab("Tilesets", tilesetsPaneGlyph, tilesetsPane);
        soundpacksPane = new SoundpacksPane();
        Glyph soundpacksPaneGlyph = fontAwesome.create(FontAwesome.Glyph.MUSIC);
        Tab soundpacks = createTab("Soundpacks", soundpacksPaneGlyph, soundpacksPane);
        worldsPane = new WorldsPane();
        Glyph worldsPaneGlyph = fontAwesome.create(FontAwesome.Glyph.SAVE);
        Tab worlds = createTab("Worlds", worldsPaneGlyph, worldsPane);
        Glyph settingsPaneGlyph = fontAwesome.create(FontAwesome.Glyph.GEAR);
        settingsPane = new SettingsPane();
        Tab settings = createTab("Settings", settingsPaneGlyph, settingsPane);
        
        tabPane = new TabPane(game, mods, tilesets, soundpacks, worlds, settings);
        setCenter(tabPane);
    }
    
    private Tab createTab(String title, Node graphic, Node content) {
        Tab tab = new Tab(title, content);
        tab.setGraphic(graphic);
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
            try {
                runnable.run();
            } catch (Exception ex) {
                logger.error("Error occured while executing parallel task", ex);
            }
            
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
