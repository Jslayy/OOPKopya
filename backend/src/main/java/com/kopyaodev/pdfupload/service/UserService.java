package com.kopyaodev.pdfupload.service;

import com.kopyaodev.pdfupload.model.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService implements UserDetailsService {

    /*  Kullanıcının şifre + rol bilgisini tutan basit kayıt sınıfı  */
    private static class UserRecord {
        final String encodedPwd;
        final Role role;

        UserRecord(String encodedPwd, Role role) {
            this.encodedPwd = encodedPwd;
            this.role = role;
        }
    }

    private final Map<String, UserRecord> users = new HashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /*  In-memory kullanıcı listesi  */
    public UserService() {
        users.put("ahmet",
            new UserRecord(passwordEncoder.encode("ahmet123"), Role.ROLE_OGRETMEN));
        users.put("mehmet",
            new UserRecord(passwordEncoder.encode("mehmet123"), Role.ROLE_OGRENCI));
        users.put("ayse",
            new UserRecord(passwordEncoder.encode("ayse123"), Role.ROLE_OGRENCI));
    }

    /*  Spring Security’nin çağırdığı asıl metot  */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserRecord rec = users.get(username);
        if (rec == null) {
            throw new UsernameNotFoundException("Kullanıcı bulunamadı: " + username);
        }

        List<GrantedAuthority> authorities =
            List.of(new SimpleGrantedAuthority(rec.role.name()));

        return new User(username, rec.encodedPwd, authorities);
    }
}
