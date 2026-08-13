package com.novacommerce.inventory.api.error;
import java.time.Instant; import java.util.Map;
public record InventoryApiError(Instant timestamp,int status,String code,String message,String path,Map<String,String> fieldErrors){}
