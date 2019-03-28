package pl.warlander.cdda.launcher.gui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
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
import pl.warlander.cdda.launcher.model.builds.BuildData;
import pl.warlander.cdda.launcher.model.builds.BuildsManager;
import pl.warlander.cdda.launcher.model.changelog.ChangelogManager;

public class GamePane extends VBox {

    private final LauncherPane parent;

    private final TextField versionField;
    private final TextField buildField;

    private final ComboBox<BuildData> buildsComboBox;
    private final WebView buildsChangelogView;

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

        ToggleGroup buildsGroup = new ToggleGroup();
        Label buildsLabel = createGridLabel("Builds: ", 0);
        RadioButton experimentalBuildsRadio = createGridRadioButton("Experimental", buildsGroup, 1, 0);
        RadioButton stableBuildsRadio = createGridRadioButton("Stable", buildsGroup, 2, 0);
        if (parent.getDirectoriesManager().getLauncherProperties().useExperimentalBuilds) {
            experimentalBuildsRadio.setSelected(true);
        }
        else {
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
        }
        else {
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
        
        
        Label availableBuildsLabel = new Label("Available builds: ");
        buildsComboBox = new ComboBox();
        buildsComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(buildsComboBox, Priority.ALWAYS);
        Button refreshButton = new Button("Refresh");
        HBox buildSelectBox = new HBox(availableBuildsLabel, buildsComboBox, refreshButton);
        buildSelectBox.setSpacing(5);
        buildSelectBox.setPadding(new Insets(5));
        buildSelectBox.setAlignment(Pos.CENTER_LEFT);
        buildSelectBox.setPrefWidth(Double.MAX_VALUE);

        buildsChangelogView = new WebView();
        buildsChangelogView.setFontScale(0.75);

        getChildren().addAll(currentVersionGrid, new Separator(), buildsGrid, buildSelectBox, buildsChangelogView);
        
        updateState();
    }
    
    private void updateState() {
        setDisable(true);
        parent.submitTask(() -> {
            updateBuilds();
            updateChangelog();
            Platform.runLater(() -> {
                setDisable(false);
            });
        });
    }
    
    private void updateBuilds() {
        try {
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
        } catch (IOException ex) {
            Logger.getLogger(GamePane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateChangelog() {
        try {
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
        } catch (IOException ex) {
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
