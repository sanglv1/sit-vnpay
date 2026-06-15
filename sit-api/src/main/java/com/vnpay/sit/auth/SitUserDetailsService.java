package com.vnpay.sit.auth;

import com.vnpay.sit.user.entity.SitUser;
import com.vnpay.sit.user.repository.SitUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SitUserDetailsService implements UserDetailsService {

    private final SitUserRepository userRepository;

    public SitUserDetailsService(SitUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SitUser user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));
        return new SitUserPrincipal(user);
    }
}
