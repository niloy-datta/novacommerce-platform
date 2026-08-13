package com.novacommerce.inventory.application;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
@Component public class ExpirationScheduler {private final InventoryService inventory;public ExpirationScheduler(InventoryService i){inventory=i;}@Scheduled(fixedDelayString="${inventory.expiration-scan-interval:PT30S}") public void expire(){inventory.expireBatch();}}
