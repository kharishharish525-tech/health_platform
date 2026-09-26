package com.ruralhealth.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Static website files are public; API endpoints require HTTP Basic auth.
 * Credentials are configurable for local deployment. Replace in-memory users
 * with persistent identity management before using real patient data.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/chart-of-accounts/**", "/api/departments/**",
                    "/api/doctors/**", "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/chart-of-accounts/**", "/api/departments/**",
                    "/api/doctors/**", "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/departments/**", "/api/doctors/**",
                    "/api/products/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
        public UserDetailsService userDetailsService(
            @Value("${app.security.admin.username:admin}") String adminUsername,
            @Value("${app.security.admin.password:admin123}") String adminPassword,
            @Value("${app.security.accountant.username:accountant}") String accountantUsername,
            @Value("${app.security.accountant.password:accountant123}") String accountantPassword) {
        UserDetails admin = User.withUsername(adminUsername)
            .password("{noop}" + adminPassword)
                .roles("ADMIN")
                .build();
        UserDetails accountant = User.withUsername(accountantUsername)
            .password("{noop}" + accountantPassword)
            .roles("ACCOUNTANT")
            .build();
        return new InMemoryUserDetailsManager(admin, accountant);
    }
}
