package pl.warlander.cdda.launcher.gui;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import javafx.scene.web.WebView;
import org.kamranzafar.jddl.DirectDownloader;
import org.kamranzafar.jddl.DownloadListener;
import org.kamranzafar.jddl.DownloadTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.warlander.cdda.launcher.model.builds.BuildData;
import pl.warlander.cdda.launcher.model.builds.BuildsManager;
import pl.warlander.cdda.launcher.model.changelog.ChangelogManager;

public class GamePane extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(GamePane.class);
    
    private static final String UPDATE_GAME_BUTTON_DOWNLOAD_TEXT = "Install selected game version";
    private static final String UPDATE_GAME_BUTTON_CANCEL_TEXT = "Cancel download";
    
    private final LauncherPane parent;

    private final TextField versionField;
    private final TextField buildField;
    
    private final Button updateGameButton;

    private final ComboBox<BuildData> buildsComboBox;
    private final WebView buildsChangelogView;
    
    private DownloadTask newVersionDownloadTask;

    public GamePane(LauncherPane parent) {
        this.parent = parent;

        Label versionLabel = createGridLabel("Version:", 0);
        versionField = createGridTextField(0);

        Label buildLabel = createGridLabel("Build:", 1);
        buildField = createGridTextField(1);

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
        currentVersionGrid.getChildren().addAll(versionLabel, versionField, buildLabel, buildField);

        Button launchGameButton = new Button("Launch game");
        launchGameButton.setPrefWidth(Double.MAX_VALUE);
        launchGameButton.getStyleClass().add("bigButton");
        VBox.setMargin(launchGameButton, new Insets(0, 10, 10, 10));

        ToggleGroup buildsGroup = new ToggleGroup();
        Label buildsLabel = createGridLabel("Builds: ", 0);
        RadioButton experimentalBuildsRadio = createGridRadioButton("Experimental", buildsGroup, 1, 0);
        RadioButton stableBuildsRadio = createGridRadioButton("Stable", buildsGroup, 2, 0);
        if (parent.getDirectoriesManager().getLauncherProperties().useExperimentalBuilds) {
            experimentalBuildsRadio.setSelected(true);
        } else {
            stableBuildsRadio.setSelected(true);
        }
        experimentalBuildsRadio.selectedProperty().addListener((ov, oldValue, newValue) -> {
            parent.getDirectoriesManager().getLauncherProperties().useExperimentalBuilds = newValue;
            updateState();
        });

        ToggleGroup graphicsGroup = new ToggleGroup();
        Label graphicsLabel = createGridLabel("Graphics: ", 1);
        RadioButton tilesGraphicsRadio = createGridRadioButton("Tiles", graphicsGroup, 1, 1);
        RadioButton cursesGraphicsRadio = createGridRadioButton("Curses", graphicsGroup, 2, 1);
        if (parent.getDirectoriesManager().getLauncherProperties().useTilesBuilds) {
            tilesGraphicsRadio.setSelected(true);
        } else {
            cursesGraphicsRadio.setSelected(true);
        }
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

        updateGameButton = new Button(UPDATE_GAME_BUTTON_DOWNLOAD_TEXT);
        updateGameButton.setPrefWidth(Double.MAX_VALUE);
        updateGameButton.getStyleClass().add("bigButton");
        VBox.setMargin(updateGameButton, new Insets(10, 10, 10, 10));
        updateGameButton.setOnAction(this::onGameUpdateRequested);

        buildsChangelogView = new WebView();
        buildsChangelogView.setFontScale(0.75);

        getChildren().addAll(currentVersionGrid, launchGameButton, new Separator(), buildsGrid, buildSelectBox, updateGameButton, buildsChangelogView);

        updateState();
    }

    private void onGameUpdateRequested(ActionEvent evt) {
        DirectDownloader dd = parent.getDownloader();
        if (newVersionDownloadTask != null) {
            newVersionDownloadTask.setCancelled(true);
            newVersionDownloadTask = null;
            return;
        }
        
        BuildData selectedBuild = buildsComboBox.getValue();
        if (selectedBuild == null) {
            return;
        }

        Path temporaryDownloadFile;
        FileOutputStream temporaryDownloadFileOutput;
        URL downloadURL;
        try {
            temporaryDownloadFile = Files.createTempFile(null, null);
            temporaryDownloadFileOutput = new FileOutputStream(temporaryDownloadFile.toFile());
            downloadURL = new URL(selectedBuild.getDownloadLink());
        } catch (IOException ex) {
            logger.error("Unable to initialize file download", ex);
            return;
        }
        
        newVersionDownloadTask = createNewVersionDownloadTask(downloadURL, temporaryDownloadFileOutput);
        dd.download(newVersionDownloadTask);
        parent.submitTask(dd);
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
                    disableNodes(buildsChangelogView, updateGameButton);
                    updateGameButton.setText(UPDATE_GAME_BUTTON_CANCEL_TEXT);
                    parent.getStatusBar().setProgress(0);
                    parent.getStatusBar().setText("Downloading " + fileName + " (0%)");
                });
            }

            public void onComplete() {
                logger.info("File download completed: " + fileName);
                Platform.runLater(() -> {
                    updateGameButton.setText(UPDATE_GAME_BUTTON_DOWNLOAD_TEXT);
                    parent.getStatusBar().setText("Ready");
                    parent.getStatusBar().setProgress(0);
                    enableNodes();
                });
            }
            public void onCancel() {
                logger.info("File download cancelled: " + fileName);
                Platform.runLater(() -> {
                    updateGameButton.setText(UPDATE_GAME_BUTTON_DOWNLOAD_TEXT);
                    parent.getStatusBar().setText("Ready");
                    parent.getStatusBar().setProgress(0);
                    enableNodes();
                });
            }
        });
    }

    private void updateState() {
        disableNodes(buildsChangelogView);
        parent.submitTask(() -> {
            updateBuilds();
            updateChangelog();
            Platform.runLater(() -> {
                enableNodes();
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
    
    private void disableNodes(Node... exceptions) {
        outer:
        for (Node node : getChildren()) {
            for (Node exception : exceptions) {
                if (node == exception) {
                    continue outer;
                }
            }
            
            node.setDisable(true);
        }
    }
    
    private void enableNodes() {
        for (Node node : getChildren()) {
            node.setDisable(false);
        }
    }

}
