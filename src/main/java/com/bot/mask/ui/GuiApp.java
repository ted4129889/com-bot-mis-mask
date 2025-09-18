package com.bot.mask.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

public class GuiApp extends Application {
    private static ConfigurableApplicationContext springContext;

    public static void setSpringContext(ApplicationContext context) {
        GuiApp.springContext = (ConfigurableApplicationContext) context;
    }

    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        loader.setControllerFactory(springContext::getBean); // 結合 Spring
        Scene scene = new Scene(loader.load(), 350, 250);
        //加入CSS
//        scene.getStylesheets().add(getClass().getResource("/styles/button-style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("遮蔽資料工具");
        primaryStage.setResizable(false);
        primaryStage.show();

        // 設置關閉事件：當視窗關閉時，關閉 Spring Boot 服務
        primaryStage.setOnCloseRequest(event -> {
            springContext.close(); // 正常關閉 Spring Boot
            Platform.exit();       // 結束 JavaFX 應用
        });


    }


}
