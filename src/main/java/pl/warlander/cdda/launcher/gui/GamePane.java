package pl.warlander.cdda.launcher.gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.web.WebView;
import org.kamranzafar.jddl.DownloadListener;
import org.kamranzafar.jddl.DownloadTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.warlander.cdda.launcher.model.builds.BuildData;
import pl.warlander.cdda.launcher.model.builds.BuildsManager;
import pl.warlander.cdda.launcher.model.changelog.ChangelogManager;

public class GamePane extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(GamePane.class);

    private static final String LAUNCH_BUTTON_LAUNCH_TEXT = "Launch game";
    private static final String LAUNCH_BUTTON_INVALID_FOLDER_TEXT = "No valid game folder found";
    private static final String LAUNCH_BUTTON_INVALID_EXECUTABLE_TEXT = "No valid game executable found";
    
    private static final String UPDATE_BUTTON_DOWNLOAD_TEXT = "Install selected game version";
    private static final String UPDATE_BUTTON_CANCEL_TEXT = "Cancel download";

    private final LauncherPane parent;

    private final TextField buildField;
    private final Button launchGameButton;
    
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

        GridPane currentVersionGrid = new GridPane();
        currentVersionGrid.setPadding(new Insets(5));
        currentVersionGrid.setHgap(5);
        currentVersionGrid.setVgap(5);
        ColumnConstraints currentVersionlabelColumn = new ColumnConstraints(60);
        currentVersionlabelColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints currentVersionfieldColumn = new ColumnConstraints();
        currentVersionfieldColumn.setHgrow(Priority.ALWAYS);
        currentVersionfieldColumn.setFillWidth(true);
        currentVersionGrid.getColumnConstraints().addAll(currentVersionlabelColumn, currentVersionfieldColumn);
        currentVersionGrid.getChildren().addAll(buildLabel, buildField);

        launchGameButton = new Button("Launch game");
        launchGameButton.setPrefWidth(Double.MAX_VALUE);
        launchGameButton.getStyleClass().add("bigButton");
        VBox.setMargin(launchGameButton, new Insets(0, 10, 10, 10));
        launchGameButton.setOnAction(this::onGameLaunchRequested);

        ToggleGroup buildsGroup = new ToggleGroup();
        Label buildsLabel = createGridLabel("Builds: ", 0);
        experimentalBuildsRadio = createGridRadioButton("Experimental", buildsGroup, 1, 0);
        stableBuildsRadio = createGridRadioButton("Stable", buildsGroup, 2, 0);
        experimentalBuildsRadio.selectedProperty().addListener((ov, oldValue, newValue) -> {
            parent.getDirectoriesManager().getLauncherProperties().useExperimentalBuilds = newValue;
            updateState();
        });

        ToggleGroup graphicsGroup = new ToggleGroup();
        Label graphicsLabel = createGridLabel("Graphics: ", 1);
        tilesGraphicsRadio = createGridRadioButton("Tiles", graphicsGroup, 1, 1);
        cursesGraphicsRadio = createGridRadioButton("Curses", graphicsGroup, 2, 1);
        tilesGraphicsRadio.selectedProperty().addListener((ov, oldValue, newValue) -> {
            parent.getDirectoriesManager().getLauncherProperties().useTilesBuilds = newValue;
            updateState();
        });

        GridPane buildsGrid = new GridPane();
        buildsGrid.setPadding(new Insets(5));
        buildsGrid.setHgap(5);
        buildsGrid.setVgap(5);
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
        refreshButton.setOnAction((evt) -> updateState());
        HBox buildSelectBox = new HBox(availableBuildsLabel, buildsComboBox, refreshButton);
        buildSelectBox.setSpacing(5);
        buildSelectBox.setPadding(new Insets(5));
        buildSelectBox.setAlignment(Pos.CENTER_LEFT);
        buildSelectBox.setPrefWidth(Double.MAX_VALUE);

        updateGameButton = new Button(UPDATE_BUTTON_DOWNLOAD_TEXT);
        updateGameButton.setPrefWidth(Double.MAX_VALUE);
        updateGameButton.getStyleClass().add("bigButton");
        VBox.setMargin(updateGameButton, new Insets(10, 10, 10, 10));
        updateGameButton.setOnAction(this::onGameUpdateRequested);

        buildsChangelogView = new WebView();
        buildsChangelogView.setFontScale(0.75);

        getChildren().addAll(currentVersionGrid, launchGameButton, new Separator(), buildsGrid, buildSelectBox, updateGameButton, buildsChangelogView);
        
        updateComponents();
        updateState();
    }
    
    private void updateComponents() {
        File currentGameFolder = parent.getDirectoriesManager().findCurrentGameFolder();
        if (currentGameFolder != null) {
            buildField.setText(currentGameFolder.getName());
        }
        else {
            buildField.setText("No game detected");
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
            Platform.runLater(() -> {
                parent.getStatusBar().setText("Making backup of current game version");
            });
            File backupFolder = parent.getDirectoriesManager().backupCurrentVersion();
            if (backupFolder == null) {
                return;
            }
            Platform.runLater(() -> {
                parent.getStatusBar().setText("Extracting " + selectedBuild.getName());
            });
            parent.getDirectoriesManager().extractAndInstallVersion(selectedBuild, temporaryDownloadFile);
            temporaryDownloadFile.delete();
            Platform.runLater(() -> {
                updateComponents();
            });
        });
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

    private void updateState() {
        launchGameButton.setDisable(true);
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
