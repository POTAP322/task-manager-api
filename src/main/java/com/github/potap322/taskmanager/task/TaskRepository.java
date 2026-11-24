package com.github.potap322.taskmanager.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task,Long> {
    Optional<Task> findByIdAndOwner_Id(Long id, Long ownerId);

}
