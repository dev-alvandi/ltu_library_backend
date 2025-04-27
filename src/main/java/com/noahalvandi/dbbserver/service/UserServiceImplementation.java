package com.noahalvandi.dbbserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noahalvandi.dbbserver.configuration.JwtProvider;
import com.noahalvandi.dbbserver.exception.UserException;
import com.noahalvandi.dbbserver.model.user.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
public class UserServiceImplementation implements UserService {


    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public UserServiceImplementation(HttpClient httpClient, ObjectMapper objectMapper, JwtProvider jwtProvider, UserRepository userRepository) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }

    @Override
    public User.UserType determineUserTypeByEmail(String email,
                                                  String firstName,
                                                  String lastName) {
        String lowerCaseEmail = email.toLowerCase();
        String domain = lowerCaseEmail.substring(lowerCaseEmail.indexOf("@") + 1);

        if (domain.contains("student")) {
            return User.UserType.STUDENT;
        } else if (domain.contains("ltu.se")) {
            return User.UserType.UNIVERSITY_STAFF;
        }

        try {
            String query = String.format("%s%%20%s", firstName, lastName);
            String url = "https://api.openalex.org/authors?search=" + query;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode results = root.get("results");

                if (results != null && !results.isEmpty()) {
                    return User.UserType.RESEARCHER;
                }
            }


        } catch (Exception e) {
            log.error("Failed to fetch researcher status from OpenAlex for {} {}: {}", firstName, lastName, e.getMessage(), e);
        }


        return User.UserType.PUBLIC;

    }

    @Override
    public User findUserProfileByJwt(String jwt) throws UserException {
        String email = jwtProvider.getEmailFromToken(jwt);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserException("No user with the email, \"%s\", found".formatted(email));
        }
        return user;
    }

    @Override
    public boolean isAdminOrLibrarian(User user) {
        return user.getUserType() == User.UserType.ADMIN || user.getUserType() == User.UserType.LIBRARIAN;
    }


    @Override
    public boolean isAdmin(User user) {
        return user.getUserType() == User.UserType.ADMIN;
    }
}
