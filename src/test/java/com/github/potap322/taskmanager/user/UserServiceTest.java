package com.github.potap322.taskmanager.user;

import com.github.potap322.taskmanager.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void register_shouldThrowConflictException_whenUsernameTaken() {
        when(userRepository.existsByUsername("ivan")).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            userService.register("ivan", "ivan@mail.ru", "password123");
        });

        assertEquals("Username already taken", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldThrowConflictException_whenEmailTaken() {
        // Логин свободен, а email ЗАНЯТ
        when(userRepository.existsByUsername("ivan")).thenReturn(false);
        when(userRepository.existsByEmail("ivan@mail.ru")).thenReturn(true);

        // Проверяем, что вылетает ConflictException
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            userService.register("ivan", "ivan@mail.ru", "password123");
        });

        // Проверяем сообщение и что save не вызывался
        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldNormalizeUsernameAndEmail() {
        // Настраиваем моки на НОРМАЛИЗОВАННЫЕ (нижний регистр) значения
        when(userRepository.existsByUsername("ivan")).thenReturn(false);
        when(userRepository.existsByEmail("ivan@mail.ru")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Вызываем метод с "грязными" данными (заглавные буквы)
        User savedUser = userService.register("IVAN", "Ivan@Mail.RU", "password123");

        // Проверяем, что сервис обращался к БД с НИЖНИМ регистром
        verify(userRepository).existsByUsername("ivan");
        verify(userRepository).existsByEmail("ivan@mail.ru");

        // Проверяем, что в сохраненном объекте тоже нижний регистр
        assertEquals("ivan", savedUser.getUsername());
        assertEquals("ivan@mail.ru", savedUser.getEmail());
    }

    @Test
    void register_shouldAlwaysAssignUserRole(){
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register("ivan", "ivan@mail.ru", "password123");

        assertEquals(UserRole.USER, user.getRole());
    }

    @Test
    void register_shouldHashPasswordBeforeSaving(){
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        userService.register("ivan", "ivan@mail.ru", "password123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        verify(passwordEncoder).encode("password123");
        assertEquals("hashedPassword", savedUser.getPassword());
    }

    @Test
    void register_shouldThrowConflictException_whenSaveFailsDueToDataIntegrity() {
        // Все проверки проходят успешно (юзернейм и email свободны)
        when(userRepository.existsByUsername("ivan")).thenReturn(false);
        when(userRepository.existsByEmail("ivan@mail.ru")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("HASHED");

        // Мок save() выбрасывает ошибку БД
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        // Проверяем, что наш try-catch превратил ошибку БД в ConflictException
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            userService.register("ivan", "ivan@mail.ru", "password123");
        });

        // Проверяем текст
        assertEquals("Username or email already taken", exception.getMessage());
    }



}
