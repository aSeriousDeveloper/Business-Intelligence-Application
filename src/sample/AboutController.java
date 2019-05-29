package sample;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {

    @FXML
    private GridPane aboutGrid;

    /**
     * Initialise about page
     * Add text
     * @param url
     * @param resourceBundle
     */
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Label aboutTitle = new Label("Business Intelligence Application");
        aboutTitle.setStyle("-fx-font-size: 16px");

        Label aboutSubtitle = new Label("An Application that collects Data from an API and then Filters & Displays it");
        aboutSubtitle.setStyle("-fx-font-size: 14px");

        Label aboutCreator = new Label("Created by Robert Lyons");
        aboutSubtitle.setStyle("-fx-font-size: 12px");

        aboutGrid.add(aboutTitle, 0, 0);
        aboutGrid.add(aboutSubtitle, 0, 1);
        aboutGrid.add(aboutCreator, 0, 2);

    }

}
