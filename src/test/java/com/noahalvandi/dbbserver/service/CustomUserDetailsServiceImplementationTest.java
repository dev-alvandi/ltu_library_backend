package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.model.user.User;
import com.noahalvandi.dbbserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceImplementationTest {
    private UserRepository userRepository;
    private CustomUserDetailsServiceImplementation userDetailsService;

    @BeforeEach
    public void setup() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new CustomUserDetailsServiceImplementation(userRepository);
    }

    @Test
    public void shouldReturnUserDetails_WhenUserExists() {
        // Arrange
        User mockUser = new User();
        mockUser.setEmail("some@fucking.email.com");
        mockUser.setPassword("securepassword");

        when(userRepository.findByEmail("some@fucking.email.com")).thenReturn(mockUser);

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("some@fucking.email.com");

        // Assert
        assertNotNull(userDetails);
        assertEquals("some@fucking.email.com", userDetails.getUsername());
        assertEquals("securepassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().isEmpty());

        verify(userRepository, times(1)).findByEmail("some@fucking.email.com");
    }

    @Test
    public void shouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail("missing@example.com")).thenReturn(null);

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@example.com")
        );

        assertTrue(exception.getMessage().contains("Username (email) not found!"));
        verify(userRepository, times(1)).findByEmail("missing@example.com");
    }

}