package dad.panels;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;


import java.net.URL;
import java.util.ResourceBundle;

public class ContactController implements Initializable {


    @FXML
    private WebView root;

    public WebView getRoot() {
        return root;
    }

    public ContactController() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ContactView.fxml"));
            loader.setController(this);
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        root = new WebView();
        URL webPageUrl = getClass().getResource("/webpage/index.html");
        if (webPageUrl != null) {
            root.getEngine().load(webPageUrl.toExternalForm());
        } else {
            System.err.println("Error: No se pudo encontrar el archivo index.html");
        }
    }
}