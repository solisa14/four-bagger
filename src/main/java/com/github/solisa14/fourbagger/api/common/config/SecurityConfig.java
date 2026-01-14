package com.github.solisa14.fourbagger.api.common.config;

import com.github.solisa14.fourbagger.api.common.security.JwtAuthenticationFilter;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration for the FourBagger API.
 *
 * <p>Configures HTTP security, authentication providers, password encoding, CORS settings, and JWT
 * authentication filter integration. Enables stateless session management with cookie-based JWT
 * authentication and provides secure access control for API endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final UserRepository userRepository;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final List<String> allowedOrigins;

  public SecurityConfig(
      UserRepository userRepository, JwtAuthenticationFilter jwtAuthenticationFilter,
      @Value("${app.security.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {
    this.userRepository = userRepository;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.allowedOrigins = allowedOrigins;
  }

  /**
   * Configures the HTTP security filter chain for the application.
   *
   * <p>Disables CSRF protection, configures CORS, sets up authorization rules allowing public
   * access to authentication endpoints, enforces stateless session management, and integrates the
   * JWT authentication filter into the security chain.
   *
   * @param http the HttpSecurity object to configure
   * @return the configured SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/**").permitAll().anyRequest().authenticated())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .headers(
            headers ->
                headers.frameOptions(
                    HeadersConfigurer.FrameOptionsConfig::disable)); // Required for H2 Console

    return http.build();
  }

  /**
   * Provides a UserDetailsService implementation that loads user data from the repository.
   *
   * <p>This service is used by Spring Security to retrieve user information during authentication.
   * It fetches user details by username from the UserRepository and throws an exception if the user
   * is not found.
   *
   * @return UserDetailsService implementation using the UserRepository
   */
  @Bean
  protected UserDetailsService userDetailsService() {
    return username ->
        userRepository
            .findUserByUsername(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with username: " + username));
  }

  /**
   * Provides the AuthenticationManager bean for handling authentication requests.
   *
   * <p>The AuthenticationManager is the main Spring Security interface for authenticating users.
   * This implementation uses the default Spring Security configuration with the configured
   * authentication providers.
   *
   * @param config the AuthenticationConfiguration to get the manager from
   * @return the configured AuthenticationManager
   * @throws Exception if the manager cannot be created
   */
  @Bean
  protected AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * Configures a DAO authentication provider for username/password authentication.
   *
   * <p>This provider uses the UserDetailsService to load user data and the PasswordEncoder to
   * verify passwords. It handles the authentication logic for login requests.
   *
   * @return configured DaoAuthenticationProvider
   */
  @Bean
  protected AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  /**
   * Provides a BCrypt password encoder for secure password hashing.
   *
   * <p>Uses BCrypt's adaptive hashing algorithm with default strength (10 rounds) to protect user
   * passwords in the database.
   *
   * @return configured BCrypt encoder instance
   */
  @Bean
  protected PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Configures CORS (Cross-Origin Resource Sharing) settings for the application.
   *
   * <p>Allows requests from specified origins (localhost:3000 and localhost:8080) with common HTTP
   * methods and headers. Credentials are allowed to support cookie-based authentication.
   *
   * @return configured CorsConfigurationSource
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
