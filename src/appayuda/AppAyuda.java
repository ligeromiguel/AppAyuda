/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appayuda;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import netscape.javascript.JSObject;

/**
 *
 * @author Miguel
 */
public class AppAyuda extends Application {

    private Scene scene;

    @Override
    public void start(Stage stage) {
        
        stage.setTitle("AppAyuda");
        scene = new Scene(new Browser(), 850, 600, Color.web("#666970"));
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}

//Clase del buscador
class Browser extends Region {
    
    //Atributos 
    private HBox toolBar;
    
    //ComboBox
    final ComboBox comboBox = new ComboBox();

    //Arrays con Iconos y URLs
    private static final String[] imageFiles = new String[]{
        "assets/Montecillos.png" ,
        "assets/Moodle.png", "assets/Facebook.png",
        "assets/Twitter.png",
        "assets/Youtube.png" ,
        "assets/Help.png"
    };
    
    private static final String[] captions = new String[]{
        "Inicio" ,
        "Moodle",
        "Facebook", 
        "Twitter",
        "Youtube" ,
        "Help"
    };
    
    private static final String[] urls = new String[]{
        "http://www.ieslosmontecillos.es/wp/",
        "http://aula.ieslosmontecillos.es",
        "https://twitter.com/",
        "https://www.facebook.com/",
        "https://www.youtube.com/",
        
        //Clase para obtener el recurso help.html
        AppAyuda.class.getResource("help.html").toExternalForm()
    };

    final ImageView selectedImage = new ImageView();
    final Hyperlink[] hpls = new Hyperlink[captions.length];
    final Image[] images = new Image[imageFiles.length];
    private boolean needDocumentationButton = false;
    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();
    final Button showPrevDoc = new Button("Toggle Previus Docs");
    final WebView smallView = new WebView();

    //Constructor
    public Browser() {
        
        getStyleClass().add("browser");

        //Para tratar los enlaces
        for (int i = 0; i < captions.length; i++) {
            Hyperlink hpl = hpls[i] = new Hyperlink(captions[i]);
            Image image = images[i]
                    = new Image(getClass().getResourceAsStream(imageFiles[i]));
            hpl.setGraphic(new ImageView(image));
            final String url = urls[i];
            final boolean addButton = (hpl.getText().equals("Documentacion"));

            //Procesa evento al pulsar sobre Hyperlink.
            hpl.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(javafx.event.ActionEvent e) {
                    needDocumentationButton = addButton;
                    webEngine.load(url);
                }
            });

        }

        //Página por defecto
        webEngine.load("http://www.ieslosmontecillos.es/wp/");
        //Añadimos el Buscador
        getChildren().add(browser);

        //ToolBar
        toolBar = new HBox();
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        toolBar.getChildren().addAll(hpls);
        toolBar.getChildren().add(createSpacer());
        toolBar.getChildren().add(comboBox);
        comboBox.setPrefWidth(170);
        comboBox.setPromptText("Historial de Búsqueda");
        toolBar.setPadding(new Insets(10, 20, 10, 10));
        
        /*comboBox.setLayoutX();
        comboBox.setTranslateX();*/
        
        //Añadimos el ToolBar
        getChildren().add(toolBar);

        //Historial de Búsqueda
        final WebHistory history = webEngine.getHistory();

        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>() {

            @Override
            public void onChanged(ListChangeListener.Change<? extends Entry> c) {
                c.next();

                for (Entry e : c.getRemoved()) {
                    comboBox.getItems().remove(e.getUrl());
                }

                for (Entry e : c.getAddedSubList()) {
                    comboBox.getItems().add(e.getUrl());
                }
            }
        });

        //Comportamiento del ComboBox
        comboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent ev) {
                int offset = comboBox.getSelectionModel().getSelectedIndex() - history.getCurrentIndex();

                history.go(offset);
            }
        });

        showPrevDoc.setOnAction(new EventHandler() {

            @Override
            public void handle(Event event) {
                webEngine.executeScript("toggleDisplay('PrevRel')");
            }

        });

        //Tamaño del WebView
        smallView.setPrefSize(120, 80);

        //Ventana emergente web, abre una nueva ventana en el navegador
        webEngine.setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {
            @Override
            public WebEngine call(PopupFeatures config) {
                smallView.setFontScale(0.8);

                if (!toolBar.getChildren().contains(smallView)) {
                    toolBar.getChildren().add(smallView);
                }

                //Devuelve WebEngine del Navegador
                return smallView.getEngine();
            }
        });
        
        //Carga Página
        webEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
                toolBar.getChildren().remove(showPrevDoc);
                JSObject win
                        = (JSObject) webEngine.executeScript("window");
                win.setMember("app", new JavaApp());
                if (newState == State.SUCCEEDED) {
                    if (needDocumentationButton) {
                        toolBar.getChildren().add(showPrevDoc);
                    }

                }
            }
        }
        );

    }

    //Proceso de interfaz de Javascript
    public class JavaApp {

        public void exit() {
            Platform.exit();
            System.exit(0);
        }
    }

    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double tbHeight = toolBar.prefHeight(w);
        layoutInArea(browser, 0, 0, w, h - tbHeight, 0, HPos.CENTER, VPos.CENTER);
        layoutInArea(toolBar, 0, h - tbHeight, w, tbHeight, 0, HPos.CENTER, VPos.CENTER);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 750;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 500;
    }

}
