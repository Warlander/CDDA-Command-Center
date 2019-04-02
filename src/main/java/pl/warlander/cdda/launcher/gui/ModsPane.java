package pl.warlander.cdda.launcher.gui;

import java.io.File;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pl.warlander.cdda.launcher.model.directories.ModInfo;
import pl.warlander.cdda.launcher.model.mods.ModType;
import pl.warlander.cdda.launcher.model.mods.ModData;

public class ModsPane extends VBox {
    
    private final LauncherPane parent;
    
    private final ObservableList<ModData> modsList;
    private final TableView modsTable;
    
    public ModsPane(LauncherPane parent) {
        this.parent = parent;
        
        modsList = FXCollections.observableArrayList();
        modsTable = new TableView(modsList);
        
        TableColumn nameColumn = new TableColumn("Name");
        nameColumn.setPrefWidth(200);
        nameColumn.setCellValueFactory(new PropertyValueFactory<ModData, String>("name"));
        
        TableColumn categoryColumn = new TableColumn("Category");
        categoryColumn.setPrefWidth(100);
        categoryColumn.setCellValueFactory(new PropertyValueFactory<ModData, String>("category"));
        
        TableColumn typeColumn = new TableColumn("Type");
        typeColumn.setPrefWidth(100);
        typeColumn.setCellValueFactory(new PropertyValueFactory<ModData, ModType>("type"));
        
        TableColumn installedColumn = new TableColumn("Installed");
        installedColumn.setPrefWidth(75);
        installedColumn.setCellValueFactory(new PropertyValueFactory<ModData, Boolean>("installed"));
        
        modsTable.getColumns().addAll(nameColumn, categoryColumn, typeColumn, installedColumn);
        VBox.setVgrow(modsTable, Priority.ALWAYS);
        
        getChildren().add(modsTable);
        
        File currentGameFolder = parent.getDirectoriesManager().findCurrentGameFolder();
        if (currentGameFolder != null) {
            ModInfo[] foundMods = parent.getDirectoriesManager().findMods(currentGameFolder);
            for (ModInfo foundMod : foundMods) {
                ModData modData = new ModData(foundMod.getName(), foundMod.getCategory(), ModType.MAINLINED, true);
                modsList.add(modData);
            }
        }
    }
    
}
