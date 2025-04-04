package com.noahalvandi.dbbserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noahalvandi.dbbserver.model.User;
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

    public UserServiceImplementation(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
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
}
