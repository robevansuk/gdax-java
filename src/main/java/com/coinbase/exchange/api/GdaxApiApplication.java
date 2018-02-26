package com.coinbase.exchange.api;

import com.coinbase.exchange.api.gui.GuiFrame;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by robevansuk on 20/01/2017.
 */
@SpringBootApplication
public class GdaxApiApplication {
    public static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(GdaxApiApplication.class)
                .headless(false).run(args);

        GuiFrame gui = ctx.getBean(GuiFrame.class);
        gui.init();
    }


}
