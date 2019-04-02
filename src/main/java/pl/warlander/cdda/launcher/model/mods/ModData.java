package pl.warlander.cdda.launcher.model.mods;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class ModData {
    
    private final ReadOnlyStringProperty nameProperty;
    private final ReadOnlyStringProperty categoryProperty;
    private final ReadOnlyObjectProperty<ModType> typeProperty;
    private final BooleanProperty installedProperty;
    
    public ModData(String name, String category, ModType source, boolean installed) {
        this.nameProperty = new SimpleStringProperty(name);
        this.categoryProperty = new SimpleStringProperty(category);
        this.typeProperty = new SimpleObjectProperty(source);
        this.installedProperty = new SimpleBooleanProperty(installed);
    }
    
    public String getName() {
        return nameProperty.get();
    }
    
    public String getCategory() {
        return categoryProperty.get();
    }
    
    public ModType getType() {
        return typeProperty.get();
    }
    
    public boolean isInstalled() {
        return installedProperty.get();
    }
    
    public void setInstalled(boolean installed) {
        installedProperty.set(installed);
    }
    
}
