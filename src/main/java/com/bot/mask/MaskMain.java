package com.bot.mask;


import com.bot.mask.config.DecryptPasswordInitializer;
import com.bot.mask.ui.GuiApp;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;


@SpringBootApplication(scanBasePackages = "com.bot")
public class MaskMain {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(MaskMain.class);
        app.addInitializers(new DecryptPasswordInitializer());

        app.setLazyInitialization(true);  // 設置全局懶加載
//        app.run(args);
        ApplicationContext context = app.run(args);

        // 設置 Spring Context 給 JavaFX
        GuiApp.setSpringContext(context);

        // 執行JavaFX 應用
        Application.launch(GuiApp.class, args);

    }


}
