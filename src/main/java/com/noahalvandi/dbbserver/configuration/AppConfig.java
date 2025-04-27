package com.noahalvandi.dbbserver.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

/**
 * The {@code AppConfig} class configures Spring Security for the application.
 * It defines the security filter chain, CORS configuration, and password encoder.
 */
@Configuration
@EnableWebSecurity
public class AppConfig {

//    @Value("${MAIL_SENDER_USERNAME}")
//    private String mailSenderUsername;
//
//    @Value("${MAIL_SENDER_PASSWORD}")
//    private String mailSenderPassword;

    private final String MAIL_SENDER_USERNAME;
    private final String MAIL_SENDER_PASSWORD;
    private final String AWS_ACCESS_KEY;
    private final String AWS_SECRET_KEY;
    private final String AWS_BUCKET_REGION;

    public AppConfig() {
        Dotenv dotenv = Dotenv.configure().load();
        this.MAIL_SENDER_USERNAME = dotenv.get("MAIL_SENDER_USERNAME");
        this.MAIL_SENDER_PASSWORD = dotenv.get("MAIL_SENDER_PASSWORD");
        this.AWS_ACCESS_KEY = dotenv.get("AWS_ACCESS_KEY");
        this.AWS_SECRET_KEY = dotenv.get("AWS_SECRET_KEY");
        this.AWS_BUCKET_REGION = dotenv.get("AWS_BUCKET_REGION");
    }

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * @param http the {@link HttpSecurity} to be configured
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs while configuring security
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new JwtTokenValidator(), BasicAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Configures CORS settings for the application.
     *
     * @return a {@link CorsConfigurationSource} providing CORS configurations
     */
    public CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.setAllowedOrigins(Arrays.asList(GlobalConstants.FRONTEND_BASE_URL));
                cfg.setAllowedMethods(Collections.singletonList("*"));
                cfg.setAllowCredentials(true);
                cfg.setAllowedHeaders(Collections.singletonList("*"));
                cfg.setExposedHeaders(Arrays.asList("Authorization"));
                cfg.setMaxAge(3600L);
                return cfg;
            }
        };
    }

    /**
     * Provides a {@link PasswordEncoder} bean for encoding passwords.
     *
     * @return a {@link PasswordEncoder} implementation using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        mailSender.setUsername(MAIL_SENDER_USERNAME);
        mailSender.setPassword(MAIL_SENDER_PASSWORD);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AmazonS3 amazonS3() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
        return AmazonS3ClientBuilder.standard()
                .withRegion(AWS_BUCKET_REGION)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }
}
