package pl.warlander.cdda.launcher.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import org.apache.commons.io.FileUtils;
import org.controlsfx.tools.Borders;
import org.kamranzafar.jddl.DownloadListener;
import org.kamranzafar.jddl.DownloadTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.warlander.cdda.launcher.model.builds.BuildData;
import pl.warlander.cdda.launcher.model.builds.BuildsManager;
import pl.warlander.cdda.launcher.model.changelog.ChangelogManager;
import pl.warlander.cdda.launcher.model.directories.GameModInfo;
import pl.warlander.cdda.launcher.model.directories.LauncherModInfo;
import pl.warlander.cdda.launcher.model.mods.ModType;
import pl.warlander.cdda.launcher.utils.TimeUtils;

public class GamePane extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(GamePane.class);

    private static final String LAUNCH_BUTTON_LAUNCH_TEXT = "Launch game";
    private static final String LAUNCH_BUTTON_INVALID_FOLDER_TEXT = "No valid game folder found";
    private static final String LAUNCH_BUTTON_INVALID_EXECUTABLE_TEXT = "No valid game executable found";
    
    private static final String UPDATE_BUTTON_DOWNLOAD_TEXT = "Install selected game version";
    private static final String UPDATE_BUTTON_CANCEL_TEXT = "Cancel download";

    private final LauncherPane parent;

    private final TextField buildField;
    private final TextField updatedField;
    private final Button launchGameButton;
    private final Button restoreBackupButton;
    
    private final RadioButton experimentalBuildsRadio;
    private final RadioButton stableBuildsRadio;
    
    private final RadioButton tilesGraphicsRadio;
    private final RadioButton cursesGraphicsRadio;
    
    private final Button updateGameButton;

    private final ComboBox<BuildData> buildsComboBox;
    private final WebView buildsChangelogView;

    private DownloadTask newVersionDownloadTask;

    public GamePane(LauncherPane parent) {
        this.parent = parent;

        Label buildLabel = createGridLabel("Build:", 0);
        buildField = createGridTextField(0);
        Label updatedLabel = createGridLabel("Installed on:", 1);
        updatedField = createGridTextField(1);

        GridPane currentVersionGrid = new GridPane();
        currentVersionGrid.setPadding(new Insets(5));
        currentVersionGrid.setHgap(5);
        currentVersionGrid.setVgap(5);
        ColumnConstraints currentVersionlabelColumn = new ColumnConstraints(80);
        currentVersionlabelColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints currentVersionfieldColumn = new ColumnConstraints();
        currentVersionfieldColumn.setHgrow(Priority.ALWAYS);
        currentVersionfieldColumn.setFillWidth(true);
        currentVersionGrid.getColumnConstraints().addAll(currentVersionlabelColumn, currentVersionfieldColumn);
        currentVersionGrid.getChildren().addAll(updatedLabel, updatedField, buildLabel, buildField);

        launchGameButton = new Button("Launch game");
        launchGameButton.setPrefWidth(Double.MAX_VALUE);
        launchGameButton.getStyleClass().add("hugeButton");
        VBox.setMargin(launchGameButton, new Insets(0, 10, 0, 10));
        launchGameButton.setOnAction(this::onGameLaunchRequested);
        
        restoreBackupButton = new Button("Restore backup");
        restoreBackupButton.setPrefWidth(Double.MAX_VALUE);
        restoreBackupButton.setFont(Font.font(Font.getDefault().getName(), FontWeight.BOLD, 12));
        restoreBackupButton.setTextFill(Color.RED);
        VBox.setMargin(restoreBackupButton, new Insets(5, 10, 5, 10));
        restoreBackupButton.setOnAction(this::onGameRestoreRequested);

        ToggleGroup buildsGroup = new ToggleGroup();
        Label buildsLabel = createGridLabel("Builds: ", 0);
        experimentalBuildsRadio = createGridRadioButton("Experimental", buildsGroup, 1, 0);
        stableBuildsRadio = createGridRadioButton("Stable", buildsGroup, 2, 0);
        experimentalBuildsRadio.selectedProperty().addListener((ov, oldValue, newValue) -> {
            parent.getDirectoriesManager().getLauncherProperties().useExperimentalBuilds = newValue;
            refreshBuilds();
        });

        ToggleGroup graphicsGroup = new ToggleGroup();
        Label graphicsLabel = createGridLabel("Graphics: ", 1);
        tilesGraphicsRadio = createGridRadioButton("Tiles", graphicsGroup, 1, 1);
        cursesGraphicsRadio = createGridRadioButton("Curses", graphicsGroup, 2, 1);
        tilesGraphicsRadio.selectedProperty().addListener((ov, oldValue, newValue) -> {
            parent.getDirectoriesManager().getLauncherProperties().useTilesBuilds = newValue;
            refreshBuilds();
        });

        GridPane buildsGrid = new GridPane();
        buildsGrid.setPadding(new Insets(5, 10, 0, 10));
        buildsGrid.setHgap(5);
        buildsGrid.setVgap(10);
        ColumnConstraints buildsLabelColumn = new ColumnConstraints(60);
        buildsLabelColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints buildsRadioColumn = new ColumnConstraints(100);
        buildsRadioColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints buildsRadioColumn2 = new ColumnConstraints(100);
        buildsRadioColumn2.setHgrow(Priority.ALWAYS);
        buildsGrid.getColumnConstraints().addAll(buildsLabelColumn, buildsRadioColumn, buildsRadioColumn2);
        buildsGrid.getChildren().addAll(buildsLabel, experimentalBuildsRadio, stableBuildsRadio, graphicsLabel, tilesGraphicsRadio, cursesGraphicsRadio);

        Label availableBuildsLabel = new Label("Available versions: ");
        buildsComboBox = new ComboBox();
        buildsComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(buildsComboBox, Priority.ALWAYS);
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction((evt) -> refreshBuilds());
        HBox buildSelectBox = new HBox(availableBuildsLabel, buildsComboBox, refreshButton);
        buildSelectBox.setSpacing(5);
        buildSelectBox.setPadding(new Insets(5, 10, 5, 10));
        buildSelectBox.setAlignment(Pos.CENTER_LEFT);
        buildSelectBox.setPrefWidth(Double.MAX_VALUE);

        updateGameButton = new Button(UPDATE_BUTTON_DOWNLOAD_TEXT);
        updateGameButton.setPrefWidth(Double.MAX_VALUE);
        updateGameButton.getStyleClass().add("bigButton");
        VBox.setMargin(updateGameButton, new Insets(0, 10, 0, 10));
        updateGameButton.setOnAction(this::onGameUpdateRequested);

        buildsChangelogView = new WebView();
        buildsChangelogView.setFontScale(0.75);
        Node changelogWithBorder = Borders.wrap(buildsChangelogView).lineBorder().title("Changelog").buildAll();

        getChildren().addAll(currentVersionGrid, launchGameButton, restoreBackupButton, new Separator(), buildsGrid, buildSelectBox, updateGameButton, changelogWithBorder);
        
        updateComponents();
        refreshBuilds();
        
        if (parent.getDirectoriesManager().getLauncherProperties().updateDatabase) {
            parent.submitTask(() -> {
                Platform.runLater(() -> parent.getStatusBar().setText("Updating database"));
                parent.getDirectoriesManager().updateDatabase();
            });
        }
    }
    
    private void updateComponents() {
        File currentGameFolder = parent.getDirectoriesManager().findCurrentGameFolder();
        if (currentGameFolder != null) {
            buildField.setText(currentGameFolder.getName());
        }
        else {
            buildField.setText("No game detected");
        }
        
        if (currentGameFolder != null) {
            LocalDateTime time = LocalDateTime.ofEpochSecond(currentGameFolder.lastModified() / 1000, 0, ZoneOffset.UTC);
            updatedField.setText(time.toString().replace("T", " ") + " (" + TimeUtils.timestampToNowString(time) + ")");
        }
        else {
            updatedField.setText("No game detected");
        }
        
        File currentGameExecutable = parent.getDirectoriesManager().findCurrentGameExecutable();
        if (currentGameExecutable != null) {
            launchGameButton.setText(LAUNCH_BUTTON_LAUNCH_TEXT);
            launchGameButton.setDisable(false);
        }
        else if (currentGameFolder == null) {
            launchGameButton.setText(LAUNCH_BUTTON_INVALID_FOLDER_TEXT);
            launchGameButton.setDisable(true);
        }
        else {
            launchGameButton.setText(LAUNCH_BUTTON_INVALID_EXECUTABLE_TEXT);
            launchGameButton.setDisable(true);
        }
        
        File backupFolder = parent.getDirectoriesManager().findBackupFolder();
        restoreBackupButton.setDisable(backupFolder == null);
        
        if (parent.getDirectoriesManager().getLauncherProperties().useExperimentalBuilds) {
            experimentalBuildsRadio.setSelected(true);
        } else {
            stableBuildsRadio.setSelected(true);
        }
        
        if (parent.getDirectoriesManager().getLauncherProperties().useTilesBuilds) {
            tilesGraphicsRadio.setSelected(true);
        } else {
            cursesGraphicsRadio.setSelected(true);
        }
    }

    private void onGameLaunchRequested(ActionEvent evt) {
        logger.info("Launching the game");
        File currentGameFolder = parent.getDirectoriesManager().findCurrentGameFolder();
        File currentGameExecutable = parent.getDirectoriesManager().findCurrentGameExecutable();
        if (currentGameExecutable == null) {
            logger.warn("Executable file not found");
            return;
        }
        
        ProcessBuilder pb = new ProcessBuilder(currentGameExecutable.getAbsolutePath());
        pb.directory(currentGameFolder);
        try {
            pb.start();
            logger.info("Game launched");
        } catch (IOException ex) {
            logger.error("Unable to launch the game", ex);
        }
    }
    
    private void onGameRestoreRequested(ActionEvent evt) {
        parent.submitTask(() -> {
            Platform.runLater(() -> {
                parent.getStatusBar().setText("Restoring backup");
            });
            parent.getDirectoriesManager().restoreBackup();
            Platform.runLater(() -> {
                updateComponents();
            });
        });
    }
    
    private void onGameUpdateRequested(ActionEvent evt) {
        if (newVersionDownloadTask != null) {
            newVersionDownloadTask.setCancelled(true);
            newVersionDownloadTask = null;
            return;
        }

        BuildData selectedBuild = buildsComboBox.getValue();
        if (selectedBuild == null) {
            return;
        }

        File temporaryDownloadFile;
        FileOutputStream temporaryDownloadFileOutput;
        URL downloadURL;
        try {
            temporaryDownloadFile = Files.createTempFile(null, null).toFile();
            temporaryDownloadFileOutput = new FileOutputStream(temporaryDownloadFile);
            downloadURL = new URL(selectedBuild.getDownloadLink());
        } catch (IOException ex) {
            logger.error("Unable to initialize file download", ex);
            return;
        }

        newVersionDownloadTask = createNewVersionDownloadTask(downloadURL, temporaryDownloadFileOutput);
        
        parent.submitDownload(newVersionDownloadTask);
        
        parent.submitTask(() -> {
            if (parent.getDirectoriesManager().findCurrentGameFolder() != null) {
                backupGame();
                if (parent.getDirectoriesManager().findBackupFolder() == null) {
                    logger.warn("No backup found, aborting update");
                    return;
                }
            }
            extractGame(selectedBuild, temporaryDownloadFile);
            if (parent.getDirectoriesManager().findBackupFolder() != null) {
                copySaves();
                copyMods();
            }
            
            updateModsInfo();
            
            Platform.runLater(() -> {
                updateComponents();
            });
        });
    }
    
    private void backupGame() {
        Platform.runLater(() -> {
            parent.getStatusBar().setText("Making backup of current game version");
        });
        parent.getDirectoriesManager().backupCurrentVersion();
    }
    
    private void extractGame(BuildData selectedBuild, File downloadedFile) {
        Platform.runLater(() -> {
            parent.getStatusBar().setText("Extracting " + selectedBuild.getName());
        });
        parent.getDirectoriesManager().extractAndInstallVersion(selectedBuild, downloadedFile);
        downloadedFile.delete();
    }
    
    private void copySaves() {
        File backupFolder = parent.getDirectoriesManager().findBackupFolder();
        File currentFolder = parent.getDirectoriesManager().findCurrentGameFolder();
        
        Platform.runLater(() -> {
            parent.getStatusBar().setText("Copying memorial");
        });
        
        File backupMemorial = parent.getDirectoriesManager().findMemorialFolder(backupFolder);
        File currentMemorial = parent.getDirectoriesManager().findMemorialFolder(currentFolder);
        try {
            FileUtils.copyDirectory(backupMemorial, currentMemorial);
        } catch (IOException ex) {
            logger.error("Unable to copy memorial", ex);
        }
        
        Platform.runLater(() -> {
            parent.getStatusBar().setText("Copying graveyard");
        });
        
        File backupGraveyard = parent.getDirectoriesManager().findGraveyardFolder(backupFolder);
        File currentGraveyard = parent.getDirectoriesManager().findGraveyardFolder(currentFolder);
        try {
            FileUtils.copyDirectory(backupGraveyard, currentGraveyard);
        } catch (IOException ex) {
            logger.error("Unable to copy graveyard", ex);
        }
        
        Platform.runLater(() -> {
            parent.getStatusBar().setText("Copying templates");
        });
        
        File backupTemplates = parent.getDirectoriesManager().findTemplatesFolder(backupFolder);
        File currentTemplates = parent.getDirectoriesManager().findTemplatesFolder(currentFolder);
        try {
            FileUtils.copyDirectory(backupTemplates, currentTemplates);
        } catch (IOException ex) {
            logger.error("Unable to copy templates", ex);
        }
        
        Platform.runLater(() -> {
            parent.getStatusBar().setText("Copying config");
        });
        
        File backupConfig = parent.getDirectoriesManager().findConfigFolder(backupFolder);
        File currentConfig = parent.getDirectoriesManager().findConfigFolder(currentFolder);
        try {
            FileUtils.copyDirectory(backupConfig, currentConfig);
        } catch (IOException ex) {
            logger.error("Unable to copy templates", ex);
        }
        
        Platform.runLater(() -> {
            parent.getStatusBar().setText("Copying saves");
        });
        
        File backupSaves = parent.getDirectoriesManager().findSavesFolder(backupFolder);
        File currentSaves = parent.getDirectoriesManager().findSavesFolder(currentFolder);
        try {
            FileUtils.copyDirectory(backupSaves, currentSaves);
        } catch (IOException ex) {
            logger.error("Unable to copy saves", ex);
        }
    }
    
    private void copyMods() {
        Platform.runLater(() -> {
            parent.getStatusBar().setText("Copying mods");
        });
        
        File currentGameFolder = parent.getDirectoriesManager().findCurrentGameFolder();
        File backupFolder = parent.getDirectoriesManager().findCurrentGameFolder();
        
        GameModInfo[] newMods = parent.getDirectoriesManager().findMods(currentGameFolder);
        GameModInfo[] oldMods = parent.getDirectoriesManager().findMods(backupFolder);
        
        outer:
        for (GameModInfo oldMod : oldMods) {
            for (GameModInfo newMod : newMods) {
                if (newMod.getName().equals(oldMod.getName())) {
                    // new or same version of mainlined mod found, no action needed
                    continue outer;
                }
            }
            
            try {
                FileUtils.copyDirectory(oldMod.getFolder(), new File(currentGameFolder, oldMod.getFolder().getName()));
            } catch (IOException ex) {
                logger.error("Unable to copy mod " + oldMod.getName(), ex);
            }
        }
    }
    
    private void updateModsInfo() {
        Platform.runLater(() -> {
            parent.getStatusBar().setText("Updating mods info");
        });
        
        File currentGameFolder = parent.getDirectoriesManager().findCurrentGameFolder();
        GameModInfo[] newMods = parent.getDirectoriesManager().findMods(currentGameFolder);
        LauncherModInfo[] oldLauncherMods = parent.getDirectoriesManager().loadLauncherModsInfo();
        ArrayList<LauncherModInfo> updatedLauncherMods = new ArrayList();
        
        outer:
        for (GameModInfo newMod : newMods) {
            for (LauncherModInfo launcherMod : oldLauncherMods) {
                if (launcherMod.getFolderName().equals(newMod.getFolder().getName())) {
                    updatedLauncherMods.add(launcherMod);
                    continue outer;
                }
            }
            updatedLauncherMods.add(new LauncherModInfo(newMod.getFolder().getName(), ModType.MAINLINED));
        }
        
        parent.getDirectoriesManager().saveLauncherModsInfo(updatedLauncherMods.toArray(LauncherModInfo[]::new));
    }

    private DownloadTask createNewVersionDownloadTask(URL downloadURL, FileOutputStream output) {
        return new DownloadTask(downloadURL, output, new DownloadListener() {
            private String fileName;
            private int size;

            public void onUpdate(int bytes, int totalDownloaded) {
                double progress = (double) totalDownloaded / size;
                String progressPercent = String.format("%.2f", progress * 100) + "%";
                Platform.runLater(() -> {
                    parent.getStatusBar().setProgress(progress);
                    parent.getStatusBar().setText("Downloading " + fileName + " (" + progressPercent + ")");
                });
            }

            public void onStart(String fileName, int size) {
                logger.info("Starting file download: " + fileName);
                this.fileName = fileName;
                this.size = size;
                Platform.runLater(() -> {
                    launchGameButton.setDisable(true);
                    updateGameButton.setText(UPDATE_BUTTON_CANCEL_TEXT);
                    parent.getStatusBar().setProgress(0);
                    parent.getStatusBar().setText("Downloading " + fileName + " (0%)");
                });
            }

            public void onComplete() {
                logger.info("File download completed: " + fileName);
                Platform.runLater(() -> {
                    updateGameButton.setText(UPDATE_BUTTON_DOWNLOAD_TEXT);
                    parent.getStatusBar().setText("Ready");
                    parent.getStatusBar().setProgress(0);
                    updateComponents();
                });
            }

            public void onCancel() {
                logger.info("File download cancelled: " + fileName);
                Platform.runLater(() -> {
                    updateGameButton.setText(UPDATE_BUTTON_DOWNLOAD_TEXT);
                    parent.getStatusBar().setText("Ready");
                    parent.getStatusBar().setProgress(0);
                    updateComponents();
                });
            }
        });
    }

    private void refreshBuilds() {
        launchGameButton.setDisable(true);
        restoreBackupButton.setDisable(true);
        parent.submitTask(() -> {
            updateBuilds();
            updateChangelog();
            Platform.runLater(() -> {
                updateComponents();
            });
        });
    }

    private void updateBuilds() {
        try {
            logger.info("Fetching build list");
            Platform.runLater(() -> {
                parent.getStatusBar().setText("Fetching new builds");
            });
            BuildsManager buildsManager = BuildsManager.createBuildsManager(parent.getDirectoriesManager().getLauncherProperties().useExperimentalBuilds);
            BuildData[] builds = buildsManager.fetchBuilds(parent.getDirectoriesManager().getLauncherProperties().useTilesBuilds);
            Platform.runLater(() -> {
                buildsComboBox.getItems().clear();
                buildsComboBox.getItems().addAll(builds);
                buildsComboBox.getSelectionModel().selectFirst();
            });
            logger.info("Build list downloaded successfully");
        } catch (IOException ex) {
            logger.error("Unable to fetch builds", ex);
        }
    }

    private void updateChangelog() {
        try {
            logger.info("Fetching changelog");
            Platform.runLater(() -> {
                parent.getStatusBar().setText("Fetching changelog");
            });

            ChangelogManager changelogManager = ChangelogManager.createChangelogManager(parent.getDirectoriesManager().getLauncherProperties().useExperimentalBuilds);
            changelogManager.downloadChangelog();
            Platform.runLater(() -> {
                parent.getStatusBar().setText("Parsing changelog");
            });
            String parsedChangelog = changelogManager.ParseChangelog();
            Platform.runLater(() -> {
                buildsChangelogView.getEngine().loadContent(parsedChangelog);
            });
            logger.info("Changelog downloaded successfully");
        } catch (IOException ex) {
            logger.error("Unable to fetch changelog", ex);
            Platform.runLater(() -> {
                buildsChangelogView.getEngine().loadContent("<p><b>Unable to load changelog</b></p>");
            });
        }
    }

    private Label createGridLabel(String text, int row) {
        Label label = new Label(text);
        label.setPrefWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_RIGHT);
        GridPane.setConstraints(label, 0, row);
        return label;
    }

    private TextField createGridTextField(int row) {
        TextField textField = new TextField();
        textField.getStyleClass().add("undecoratedTextField");
        textField.setEditable(false);
        textField.setText("Loading, please wait");
        GridPane.setConstraints(textField, 1, row);
        return textField;
    }

    private RadioButton createGridRadioButton(String text, ToggleGroup group, int column, int row) {
        RadioButton radioButton = new RadioButton(text);
        radioButton.setToggleGroup(group);
        GridPane.setConstraints(radioButton, column, row);
        return radioButton;
    }

}
