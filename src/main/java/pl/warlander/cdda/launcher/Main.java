package pl.warlander.cdda.launcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.warlander.cdda.launcher.gui.LauncherPane;

public class Main extends Application {
    
    private static final String PROGRAM_NAME = "CDDA Command Center";
    private static final String PROGRAM_VERSION = "0.1.0";
    
    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) throws Exception {
        LauncherPane root = new LauncherPane();
        
        Scene scene = new Scene(root, 400, 600);
        scene.getStylesheets().add("styles/defaultStyle.css");
        
        primaryStage.setTitle(PROGRAM_NAME + " " + PROGRAM_VERSION);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest((evt) -> System.exit(0));
        primaryStage.show();
    }
    
}
