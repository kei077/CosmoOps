package com.cosmic.tracker.authTests;

import com.cosmic.tracker.Users.UserRepository;
import com.cosmic.tracker.Users.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)            
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks                               
    private UserService userService;

    @Test
    void saveUser_encodesPasswordAndSaves() {
        String username       = "testUser";
        String rawPassword    = "password";
        String encodedPassword= "encodedPassword";
        String email          = "test@example.com";

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        userService.save(username, rawPassword, email);

        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).save(argThat(u ->
                username.equals(u.getUsername()) &&
                encodedPassword.equals(u.getPassword()) &&
                email.equals(u.getEmail())
        ));
    }
}
