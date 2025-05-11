 package com.kopyaodev.pdfupload.config;

 import com.kopyaodev.pdfupload.security.JwtAuthFilter;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.security.authentication.AuthenticationManager;
 import org.springframework.security.authentication.AuthenticationProvider;
 import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
 import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// Eğer Spring Boot 3.x kullanıyorsan aşağıdaki satırı aktif et ve üsttekini sil.
 // import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
 import org.springframework.security.config.annotation.web.builders.HttpSecurity;
 import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
 import org.springframework.security.config.http.SessionCreationPolicy;
 import org.springframework.security.core.userdetails.UserDetailsService;
 import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
 import org.springframework.security.crypto.password.PasswordEncoder;
 import org.springframework.security.web.SecurityFilterChain;
 import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
 import org.springframework.web.cors.CorsConfiguration;
 import org.springframework.web.cors.CorsConfigurationSource;
 import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
 
 import java.util.Arrays;
 
 @Configuration
 @EnableWebSecurity
 @EnableMethodSecurity
 public class SecurityConfig {
 
     private final JwtAuthFilter jwtAuthFilter;
     private final UserDetailsService userDetailsService;
 
     public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsService userDetailsService) {
         this.jwtAuthFilter = jwtAuthFilter;
         this.userDetailsService = userDetailsService;
     }
 
     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
         http
             .cors().and()
             .csrf().disable()
             .authorizeHttpRequests(authz -> authz
                 .requestMatchers("/api/auth/**").permitAll()
                 .requestMatchers("/error").permitAll()
                 // ---- Rol bazlı endpoint yetkilendirme ----
                 .requestMatchers("/api/ogretmen/**").hasRole("OGRETMEN")
                 .requestMatchers("/api/ogrenci/**").hasRole("OGRENCI")
                 // -----------------------------------------
                 .anyRequest().authenticated()
             )
             .sessionManagement()
             .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
             .and()
             .authenticationProvider(authenticationProvider())
             .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
 
         return http.build();
     }
 
     @Bean
     public AuthenticationProvider authenticationProvider() {
         DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
         provider.setUserDetailsService(userDetailsService);
         provider.setPasswordEncoder(passwordEncoder());
         return provider;
     }
 
     @Bean
     public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
         return config.getAuthenticationManager();
     }
 
     @Bean
     public PasswordEncoder passwordEncoder() {
         return new BCryptPasswordEncoder();
     }
 
     /* --------- CORS Ayarı --------- */
     @Bean
     public CorsConfigurationSource corsConfigurationSource() {
         CorsConfiguration configuration = new CorsConfiguration();
         configuration.setAllowedOrigins(Arrays.asList("http://localhost:5500", "http://127.0.0.1:5500"));
         configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
         configuration.setExposedHeaders(Arrays.asList("Authorization"));
         configuration.setAllowCredentials(true);
 
         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
         source.registerCorsConfiguration("/**", configuration);
         return source;
     }
 }