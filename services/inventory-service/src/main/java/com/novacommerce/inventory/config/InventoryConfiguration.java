package com.novacommerce.inventory.config;
import java.time.Clock; import org.springframework.boot.context.properties.EnableConfigurationProperties; import org.springframework.context.annotation.*;
@Configuration @EnableConfigurationProperties(InventoryProperties.class)
public class InventoryConfiguration { @Bean Clock inventoryClock(){return Clock.systemUTC();} }
