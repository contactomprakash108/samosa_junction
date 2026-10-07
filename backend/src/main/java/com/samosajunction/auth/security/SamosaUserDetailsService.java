package com.samosajunction.auth.security;

import com.samosajunction.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI-ASSISTED: Cursor
// PROMPT: Add UserDetailsService for AuthenticationManager login flow
// ACCEPTED-BY: omprakash
@Service
public class SamosaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public SamosaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username)
                .map(UserPrincipal::forCredentialVerification)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}
