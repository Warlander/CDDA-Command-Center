package pl.warlander.cdda.launcher.gui;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import pl.warlander.cdda.launcher.model.builds.BuildData;
import pl.warlander.cdda.launcher.model.builds.BuildsManager;
import pl.warlander.cdda.launcher.model.builds.ExperimentalBuildsManager;
import pl.warlander.cdda.launcher.model.changelog.ChangelogManager;
import pl.warlander.cdda.launcher.model.changelog.ExperimentalChangelogManager;

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

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5));
        grid.setHgap(5);
        grid.setVgap(5);
        ColumnConstraints labelColumn = new ColumnConstraints(60);
        labelColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setFillWidth(true);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);
        grid.getChildren().addAll(versionLabel, versionField, buildLabel, buildField);

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

        getChildren().addAll(grid, new Separator(), buildSelectBox, buildsChangelogView);

        parent.submitTask(() -> {
            updateBuilds();
            updateChangelog();
        });
    }
    
    private void updateBuilds() {
        boolean experimental = true;
        boolean tiles = true;
        try {
            Platform.runLater(() -> {
                parent.getStatusBar().setText("Fetching new builds");
            });
            BuildsManager buildsManager = BuildsManager.createBuildsManager(experimental);
            BuildData[] builds = buildsManager.fetchBuilds(tiles);
            Platform.runLater(() -> {
                buildsComboBox.getItems().addAll(builds);
                buildsComboBox.getSelectionModel().selectFirst();
            });
        } catch (IOException ex) {
            Logger.getLogger(GamePane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateChangelog() {
        boolean experimental = true;
        try {
            Platform.runLater(() -> {
                parent.getStatusBar().setText("Fetching changelog");
            });

            ChangelogManager changelogManager = ChangelogManager.createChangelogManager(experimental);
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

}
