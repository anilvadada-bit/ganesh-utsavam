package com.ganesh.ganesh_utsavam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder) {

        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                        "/",
                        "/index.html",
                        "/app.js",
                        "/style.css",
                        "/ganesh.jpg",
                        "/favicon.ico",
                        "/error",
                        "/csrf"
                ).permitAll()

                // Public Razorpay payment APIs
                .requestMatchers(
                        "/api/payments/**"
                ).permitAll()

                // Admin login page
                .requestMatchers(
                        "/admin-login.html"
                ).permitAll()

                // Admin area
                .requestMatchers(
                        "/admin.html",
                        "/api/admin/**"
                ).hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
    .loginPage("/admin-login.html")
    .loginProcessingUrl("/login")
    .defaultSuccessUrl("/admin.html", true)
    .failureUrl("/admin-login.html?error")
    .permitAll()
)

           

            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )

            .csrf(csrf -> csrf

                .csrfTokenRepository(
                        CookieCsrfTokenRepository
                                .withHttpOnlyFalse()
                )

                // Payment APIs are public and use Razorpay
                // signature verification instead of admin session auth.
                .ignoringRequestMatchers(
                        "/api/payments/**"
                )
            );

        return http.build();
    }
}