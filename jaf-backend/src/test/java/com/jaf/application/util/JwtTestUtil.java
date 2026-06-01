package com.jaf.application.util;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.enums.Cargo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

public class JwtTestUtil {

    public static Authentication createMockAuthentication(String email, Cargo cargo) {
        List<SimpleGrantedAuthority> authorities = cargo.getPermissoes().stream()
                .map(permissao -> new SimpleGrantedAuthority(permissao.name()))
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(email, null, authorities);
    }

    public static String generateTestToken(GerenciadorTokenJwt jwtManager, String email, Cargo cargo) {
        Authentication authentication = createMockAuthentication(email, cargo);
        return jwtManager.generateToken(authentication);
    }
}
