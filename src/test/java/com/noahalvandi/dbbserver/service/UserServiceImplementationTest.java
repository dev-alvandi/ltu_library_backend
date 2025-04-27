package com.noahalvandi.dbbserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noahalvandi.dbbserver.configuration.JwtProvider;
import com.noahalvandi.dbbserver.model.user.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplementationTest {

    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private UserRepository userRepository;
    private JwtProvider jwtProvider;
    private UserServiceImplementation userService;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        objectMapper = new ObjectMapper(); // real ObjectMapper works fine here
        userService = new UserServiceImplementation(httpClient, objectMapper, jwtProvider, userRepository);
    }

    @Test
    void shouldReturnResearcher_WhenOpenAlexReturnsResults() throws Exception {
        String firstName = "Jorgen";
        String lastName = "Nilsson";
        String email = "jorgen.nilsson@something.se";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        String body = "{\"results\":[{\"id\":\"some-id\"}]}";

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        User.UserType result = userService.determineUserTypeByEmail(email, firstName, lastName);
        assertEquals(User.UserType.RESEARCHER, result);
    }

    @Test
    void shouldReturnStudent_WhenEmailContainsStudentAfterAtSign() {
        String email = "mohghi@student.ltu.se";
        User.UserType result = userService.determineUserTypeByEmail(email, "Noah", "Alvandi");
        assertEquals(User.UserType.STUDENT, result);
    }

    @Test
    void shouldReturnPublic_WhenEmailContainsStudentBeforeAtSign() {
        String email = "mohghi-3.student@somthing.se";
        User.UserType result = userService.determineUserTypeByEmail(email, "Noah", "Alvandi");
        assertEquals(User.UserType.PUBLIC, result);
    }

    @Test
    void shouldReturnUniversityStaff_WhenEmailIsLtuStaff() {
        String email = "teacher@ltu.se";
        User.UserType result = userService.determineUserTypeByEmail(email, "Anna", "Karlsson");
        assertEquals(User.UserType.UNIVERSITY_STAFF, result);
    }

    @Test
    void shouldReturnPublic_WhenOpenAlexReturnsEmptyResults() throws Exception {
        String email = "someone@gmail.com";
        String firstName = "Test";
        String lastName = "User";

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        String body = "{\"results\":[]}";

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        User.UserType result = userService.determineUserTypeByEmail(email, firstName, lastName);
        assertEquals(User.UserType.PUBLIC, result);
    }

    @Test
    void shouldReturnPublic_WhenHttpClientThrowsException() throws Exception {
        String email = "Anna.peTtersSon@gmail.com";
        String firstName = "Anna";
        String lastName = "Pettersson";

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("Network error"));

        User.UserType result = userService.determineUserTypeByEmail(email, firstName, lastName);
        assertEquals(User.UserType.PUBLIC, result);
    }

}