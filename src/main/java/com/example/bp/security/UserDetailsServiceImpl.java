package com.example.bp.security;

import com.example.bp.domain.User;
import com.example.bp.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security가 인증할 때 "이 이메일의 사용자가 누구인지" 물어보면 답해주는 클래스.
 * 이메일로 DB에서 사용자를 찾아 로그인에 필요한 정보({@link SecurityPrincipal})로 변환한다.
 * 없으면 인증 실패로 처리된다 (PRD §6.1).
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("No user for email: " + email);
        }
        return SecurityPrincipal.from(user);
    }
}
