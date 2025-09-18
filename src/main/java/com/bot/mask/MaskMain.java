package com.bot.mask;


import com.bot.mask.config.DecryptPasswordInitializer;
import com.bot.mask.ui.GuiApp;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication(scanBasePackages = {
        "com.bot.mask",
        "com.bot.txcontrol.util"
})

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

//        String tem = "INSERT INTO botlddb_sync.dbo.db_ihtx_dds (db_ihtx_crdb_4,db_ihtx_crdb_3,db_ihtx_crdb_2,db_ihtx_kinbr,db_ihtx_crdb_1,db_ihtx_txcd,db_ihtx_actno,db_ihtx_lmtg,db_ihtx_txtno,db_ihtx_irat_3,db_ihtx_irat_2,db_ihtx_irat_1,db_ihtx_brno,db_ihtx_curcd,db_ihtx_ieday_1,db_ihtx_irat_4,db_ihtx_amttyp_2,db_ihtx_amttyp_1,db_ihtx_ieday_4,db_ihtx_ieday_3,db_ihtx_amttyp_4,db_ihtx_ieday_2,db_ihtx_amttyp_3,db_ihtx_spcd,db_ihtx_tlrno,db_ihtx_clsprd,db_ihtx_amount_4,db_ihtx_ovtrsn,db_ihtx_sapno,db_ihtx_hcode,db_ihtx_stxno,db_ihtx_isday_4,my_rsn,db_ihtx_amount_2,db_ihtx_isday_3,db_ihtx_isday_2,db_ihtx_amount_3,db_ihtx_isday_1,db_ihtx_amount_1,db_ihtx_txtime,db_ihtx_txday,db_ihtx_sqno,db_ihtx_trpact,db_ihtx_dscpt) VALUES (1,1,1,15,1,N'S22',N'9154097466',N' ',76501,0.00000,0.00000,0.00000,9,0,0,0.00000,2,1,0,0,4,990101,3,0,N'1 ',0,0.00,0,154,0,N'  ',0,N'44          ',127.00,0,981201,3.00,0,0.00,110920,1010223,6,N'0',N'C    ');";
//        String result = tem.replaceAll("\\b(bot\\w*?db)_sync\\.dbo\\.", "misbh_db.$1.");
//        System.out.println(result);
    }


}
