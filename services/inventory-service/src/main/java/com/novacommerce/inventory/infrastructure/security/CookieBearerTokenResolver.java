package com.novacommerce.inventory.infrastructure.security;
import jakarta.servlet.http.*; import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver; import org.springframework.stereotype.Component;
@Component public class CookieBearerTokenResolver implements BearerTokenResolver { public String resolve(HttpServletRequest request){Cookie[] cookies=request.getCookies();if(cookies!=null)for(Cookie cookie:cookies)if("NC_ACCESS".equals(cookie.getName()))return cookie.getValue();return null;} }
