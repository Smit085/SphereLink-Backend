package com.spherelink.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.spherelink.config.GoogleConfig;
import com.spherelink.exception.ResourceNotFoundException;
import com.spherelink.model.FileRecord;
import com.spherelink.model.User;
import com.spherelink.model.User.AuthProvider;
import com.spherelink.repository.UserRepository;
import com.spherelink.repository.ViewRepository;
import com.spherelink.service.FileService;
import com.spherelink.service.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/spherelink")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final GoogleConfig googleConfig;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    public UserController(GoogleConfig googleConfig, FileService fileService) {
        this.googleConfig = googleConfig;
        this.fileService = fileService;
    }
    

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            logger.info("Fetching all users");
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching all users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error fetching users"));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        return ResponseEntity.ok(Collections.singletonMap("message", "Tested successfully"));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        try {
            logger.info("Creating new user: {}", user);
//            User savedUser = userRepository.save(user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User created successfully");
//            response.put("user", savedUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error creating user"));
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        try {
            logger.info("Fetching user with ID: {}", id);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
            return ResponseEntity.ok(user);
        } catch (ResourceNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error fetching user"));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        try {
            logger.info("Fetching user with email: {}", email);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
            return ResponseEntity.ok(user);
        } catch (ResourceNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error fetching user by email"));
        }
    }

//    @PutMapping("/users/{id}")
//    public ResponseEntity<?> updateUser(@PathVariable UUID id, @Valid @RequestBody User userDetails) {
//        try {
//            logger.info("Updating user with ID: {}", id);
//            User user = userRepository.findById(id)
//                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
//
//            user.setFirstName(userDetails.getFirstName());
//            user.setLastName(userDetails.getLastName());
//            user.setEmail(userDetails.getEmail());
//            user.setPassword(userDetails.getPassword());
//            user.setPhoneNumber(userDetails.getPhoneNumber());
//
//            return ResponseEntity.ok(userRepository.save(user));
//        } catch (ResourceNotFoundException e) {
//            logger.error("User not found: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(Collections.singletonMap("error", e.getMessage()));
//        } catch (Exception e) {
//            logger.error("Error updating user: {}", e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Collections.singletonMap("error", "Error updating user"));
//        }
//    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        try {
            logger.info("Deleting user with ID: {}", id);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

            userRepository.delete(user);
            return ResponseEntity.ok(Collections.singletonMap("deleted", Boolean.TRUE));
        } catch (ResourceNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error deleting user"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        try {
            logger.info("Signing up user with email: {}", user.getEmail());

            Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
            if (existingUser.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Collections.singletonMap("message", "User already exists"));
            }

            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setProvider(AuthProvider.LOCAL);
            
            User savedUser = userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Signup successful");
            response.put("user", savedUser);
            logger.info("Signup successful");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.error("Error during signup: {}", e.getMessage());
            System.out.print("Error during signup: {}\"" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error during signup"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String emailId = credentials.get("email");
            String password = credentials.get("password");

            logger.info("User login attempt: {}", emailId);

            Optional<User> userOptional = userRepository.findByEmail(emailId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("message", "Invalid email."));
            }

            User user = userOptional.get();
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("message", "Invalid password."));
            }

            String jwtToken = jwtService.generateToken(user.getEmail());

            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "token", jwtToken
            ));
        } catch (Exception e) {
            logger.error("Error during login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error during login"));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody Map<String, String> requestBody) {
        String idTokenString = requestBody.get("idToken");

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleConfig.getId())) // Use GoogleConfig
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                String email = payload.getEmail();
                String google_id = payload.getSubject();
                String firstName = (String) payload.get("given_name");
                String lastName = (String) payload.get("family_name");
                String pictureUrl = (String) payload.get("picture");

                Optional<User> user = userRepository.findByEmail(email);
                if (user.isEmpty()) {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(firstName);
                    newUser.setLastName(lastName);
                    newUser.setProvider(AuthProvider.GOOGLE);
                    newUser.setGoogleId(google_id);
                    newUser.setProfileImagePath(pictureUrl);
                    userRepository.save(newUser);
                    user = Optional.of(newUser);
                }

                String jwtToken = jwtService.generateToken(user.get().getEmail());

                return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "token", jwtToken,
                    "user", user.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("error", "Invalid Google ID Token"));
            }
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Google authentication failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Google authentication failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during Google authentication: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error during Google authentication"));
        }
    }
    
    @PutMapping(value = "/users/profile", consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<?> updateUserProfile(
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                logger.error("No authenticated user found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("error", "Authentication required"));
            }
            String email = auth.getName();
            logger.info("Updating profile for user: {}", email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

            if (firstName != null && !firstName.trim().isEmpty()) {
                user.setFirstName(firstName);
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                user.setLastName(lastName);
            }
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                user.setPhoneNumber(phoneNumber);
            }

            String oldProfileImagePath = user.getProfileImagePath();
            if (profileImage != null && !profileImage.isEmpty()) {
                FileRecord fileRecord = fileService.saveFile(profileImage, "user_" + user.getUserId(), true);
                if (fileRecord != null) {
                    user.setProfileImagePath(fileRecord.getFilePath());
                    logger.info("Profile image saved for user {}: {}", email, fileRecord.getFilePath());

                    int updatedViews = viewRepository.updateCreatorProfileImagePath(
                            user.getUserId(), fileRecord.getFilePath());
                    logger.info("Updated creatorProfileImagePath for {} views for user: {}", updatedViews, email);

                    // Optionally, clean up the old profile image file if it exists
                    if (oldProfileImagePath != null && !oldProfileImagePath.isEmpty()) {
                        try {
                            fileService.deleteFile(oldProfileImagePath);
                            logger.info("Deleted old profile image: {}", oldProfileImagePath);
                        } catch (Exception e) {
                            logger.warn("Failed to delete old profile image: {}", oldProfileImagePath, e);
                        }
                    }
                } else {
                    logger.warn("Failed to save profile image for user: {}", email);
                }
            }

            User updatedUser = userRepository.save(user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("user", updatedUser);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (IOException e) {
            logger.error("Error saving profile image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error saving profile image"));
        } catch (Exception e) {
            logger.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error updating profile"));
        }
    }
}
