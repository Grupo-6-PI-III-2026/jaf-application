package com.jaf.application.controller.auth;


import com.jaf.application.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Long>> login(@RequestBody(required = false) LoginRequest request) {
        String email = request != null ? request.email() : null;
        String password = request != null ? request.password() : null;

        Long id = authService.autenticar(email, password);
        return ResponseEntity.ok(Map.of("id", id));
    }

    public record LoginRequest(String email, String password) {}

}
