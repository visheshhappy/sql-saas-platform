package com.thp.sqlsaas.persistence.service;

import com.thp.sqlsaas.persistence.entity.User;
import com.thp.sqlsaas.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Service for managing users and their roles.
 * This is the SOURCE OF TRUTH for user roles - never trust client input!
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get user by userId and tenantId.
     * Returns user with their roles loaded.
     */
    public Optional<User> getUser(String userId, String tenantId) {
        return userRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Get user roles from database.
     * THIS IS THE AUTHORITATIVE SOURCE - never trust client input!
     *
     * @param userId User identifier
     * @param tenantId Tenant identifier
     * @return Set of role names for the user
     * @throws SecurityException if user not found (prevents privilege escalation)
     */
    public Set<String> getUserRoles(String userId, String tenantId) {
        logger.debug("Fetching roles for user: {}, tenant: {}", userId, tenantId);

        User user = userRepository.findByUserIdAndTenantId(userId, tenantId)
            .orElseThrow(() -> {
                logger.error("User not found: {} in tenant: {}", userId, tenantId);
                return new SecurityException("User not found or not authorized");
            });

        Set<String> roles = user.getRoleNames();
        logger.debug("User {} has roles: {}", userId, roles);

        return roles;
    }

    /**
     * Check if user exists in tenant.
     */
    public boolean userExists(String userId, String tenantId) {
        return userRepository.existsByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String userId, String tenantId, String roleName) {
        Set<String> roles = getUserRoles(userId, tenantId);
        return roles.contains(roleName);
    }

    /**
     * Check if user has admin role.
     */
    public boolean isAdmin(String userId, String tenantId) {
        return hasRole(userId, tenantId, "admin");
    }
}