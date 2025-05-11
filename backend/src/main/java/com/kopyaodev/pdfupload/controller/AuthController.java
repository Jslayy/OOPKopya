package com.kopyaodev.pdfupload.controller;



import com.kopyaodev.pdfupload.model.LoginRequest;
import com.kopyaodev.pdfupload.model.LoginResponse;
import com.kopyaodev.pdfupload.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        UserDetails ud = (UserDetails) auth.getPrincipal();
        String jwt = jwtService.generateToken(ud);

        List<String> roles = ud.getAuthorities()
                               .stream()
                               .map(GrantedAuthority::getAuthority)
                               .toList();

        return ResponseEntity.ok(new LoginResponse(jwt, roles));
    }
}