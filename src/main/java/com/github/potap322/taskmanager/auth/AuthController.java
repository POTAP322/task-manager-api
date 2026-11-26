package com.github.potap322.taskmanager.auth;

import com.github.potap322.taskmanager.auth.dto.RegisterRequest;
import com.github.potap322.taskmanager.user.User;
import com.github.potap322.taskmanager.user.UserService;
import com.github.potap322.taskmanager.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username(),
                request.email(),
                request.password()
        );

        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }


}
