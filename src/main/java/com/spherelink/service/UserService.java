package com.spherelink.service;

import com.spherelink.exception.ResourceNotFoundException;
import com.spherelink.model.User;
import com.spherelink.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by ID
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    // Save a new user
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // Update user
    public User updateUser(UUID id, User userDetails) {
        User user = getUserById(id);

        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setEmail(userDetails.getEmail());

        return userRepository.save(user);
    }

    // Delete user
    public void deleteUser(UUID id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    public UUID getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
    
    public String getUserNameById(UUID userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return user.getFirstName() + " " + user.getLastName();
        }
        return "Anonymous";
    }

}