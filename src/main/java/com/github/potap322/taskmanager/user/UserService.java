package com.github.potap322.taskmanager.user;
import com.github.potap322.taskmanager.common.exception.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String username, String email, String rawPassword){
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if(userRepository.existsByUsername(normalizedUsername)){
            throw new ConflictException("Username already taken");
        }
        if(userRepository.existsByEmail(normalizedEmail)){
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.USER);

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Username or email already taken");
        }

        return user;
    }
}
