package com.samosajunction.user.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "recipient_name", length = 120)
    private String recipientName;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String state;

    @Column(length = 16)
    private String pincode;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    protected User() {
    }

    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.enabled = true;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPincode() {
        return pincode;
    }

    public boolean hasDefaultAddress() {
        return notBlank(recipientName)
                && notBlank(addressLine1)
                && notBlank(city)
                && notBlank(state)
                && notBlank(pincode);
    }

    public void updateProfile(
            String fullName,
            String phone,
            String recipientName,
            String addressLine1,
            String city,
            String state,
            String pincode
    ) {
        if (notBlank(fullName)) {
            this.fullName = fullName.trim();
        }
        this.phone = blankToNull(phone);
        this.recipientName = blankToNull(recipientName);
        this.addressLine1 = blankToNull(addressLine1);
        this.city = blankToNull(city);
        this.state = blankToNull(state);
        this.pincode = blankToNull(pincode);
    }

    public void replaceDefaultAddress(
            String recipientName,
            String addressLine1,
            String city,
            String state,
            String pincode
    ) {
        this.recipientName = blankToNull(recipientName);
        this.addressLine1 = blankToNull(addressLine1);
        this.city = blankToNull(city);
        this.state = blankToNull(state);
        this.pincode = blankToNull(pincode);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
