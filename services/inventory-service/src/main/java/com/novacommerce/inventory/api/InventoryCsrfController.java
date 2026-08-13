package com.novacommerce.inventory.api;
import org.springframework.security.web.csrf.CsrfToken; import org.springframework.web.bind.annotation.*;
@RestController public class InventoryCsrfController {@GetMapping("/api/v1/inventory/csrf") public CsrfResponse csrf(CsrfToken t){return new CsrfResponse(t.getToken(),t.getHeaderName());}public record CsrfResponse(String token,String headerName){} }
