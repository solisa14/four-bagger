package com.github.solisa14.fourbagger.api.security;

import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Provides Spring beans for application security components.
 *
 * <p>Configures the security filter chain, authentication providers, and password encoding.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final UserRepository userRepository;
  private final List<String> allowedOrigins;
  private final boolean h2ConsoleEnabled;
  private final ApiAuthenticationEntryPoint authenticationEntryPoint;
  private final ApiAccessDeniedHandler accessDeniedHandler;

  /**
   * Constructs a SecurityConfig.
   *
   * @param userRepository the user repository
   * @param allowedOrigins the list of allowed CORS origins
   * @param h2ConsoleEnabled whether the H2 console should be enabled
   * @param authenticationEntryPoint the entry point for authentication errors
   * @param accessDeniedHandler the handler for access denied errors
   */
  public SecurityConfig(
      UserRepository userRepository,
      @Value("${app.cors.allowed-origins}") List<String> allowedOrigins,
      @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled,
      ApiAuthenticationEntryPoint authenticationEntryPoint,
      ApiAccessDeniedHandler accessDeniedHandler) {
    this.userRepository = userRepository;
    this.allowedOrigins = allowedOrigins;
    this.h2ConsoleEnabled = h2ConsoleEnabled;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  /**
   * Configures the security filter chain.
   *
   * <p>Disables CSRF (stateless API), configures CORS, sets public endpoints, and adds the JWT
   * authentication filter.
   *
   * @param http the HttpSecurity object to configure
   * @param jwtAuthenticationFilter the JWT authentication filter
   * @return the configured SecurityFilterChain
   * @throws Exception if an error occurs during configuration
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers("/api/v1/auth/**").permitAll();
              if (h2ConsoleEnabled) {
                auth.requestMatchers("/h2-console/**").permitAll();
              }
              auth.anyRequest().authenticated();
            })
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .headers(
            headers -> {
              if (h2ConsoleEnabled) {
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
              }
              headers.referrerPolicy(
                  referrer ->
                      referrer.policy(
                          ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
            });

    return http.build();
  }

  /**
   * Provides the BCrypt password encoder bean.
   *
   * @return a new BCryptPasswordEncoder instance
   */
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Provides the UserDetailsService bean, which loads user-specific data during authentication.
   *
   * @return the UserDetailsService instance
   */
  @Bean
  public UserDetailsService userDetailsService() {
    return username ->
        userRepository
            .findUserByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  /**
   * Provides the AuthenticationProvider bean, configured with the UserDetailsService and password
   * encoder.
   *
   * @return the AuthenticationProvider instance
   */
  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  /**
   * Provides the AuthenticationManager bean.
   *
   * @param config the authentication configuration
   * @return the AuthenticationManager instance
   * @throws Exception if an error occurs while retrieving the manager
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * Provides the CorsConfigurationSource bean to configure CORS settings.
   *
   * @return the CorsConfigurationSource instance
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
