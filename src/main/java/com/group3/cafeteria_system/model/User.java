package com.group3.cafeteria_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId; // PK value, take note...

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false) // we have to hash the user passwords so (standard security for passwords stored in DB's)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String email;

    // Role values: STUDENT, STAFF, ADMIN
    @Column(nullable = false)
    private String role;

    @Column(name = "created_at")  // when account was created
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Constructors
    public User() {}

    // Minimal constructor for demo phase seed users
    public User(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Full constructor for registration (Term 3)
    public User(String username, String passwordHash, String firstName, String lastName, String email, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
    }

    // Business logic methods
    public boolean isStaff() {
        return role.equals("STAFF") || role.equals("ADMIN");
    }

    public boolean isAdmin() {
        return role.equals("ADMIN");
    }

    // Returns display name — falls back to username
    // if first/last name not set
    public String getDisplayName() {
        if (firstName != null && !firstName.isEmpty()) {
            return firstName + " " + lastName;
        }
        return username;
    }

    // Getters and Setters
    public Long getUserId() { return userId; } // PK for table/database
    public String getUsername() { return username; }
    public String getLastName() { return lastName; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }


    public void setUsername(String username) {this.username = username;}
    public void setPasswordHash(String passwordHash) {this.passwordHash = passwordHash;}
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
}