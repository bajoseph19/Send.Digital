package com.varscon.sendcorp.SendCorp.security;


import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import com.varscon.sendcorp.SendCorp.accounts.repositories.UserRepository;
import com.varscon.sendcorp.SendCorp.accounts.services.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AppUserDetails implements UserDetailsService {

  private UserRepository userRepository;

  @Override
  public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    final Optional<UserModel> userRes = userRepository.findByEmail(username);
    if(userRes.isPresent()){
      UserModel user = userRes.get();

      return org.springframework.security.core.userdetails.User//
              .withUsername(username)//
              .password(user.getPassword())//
              .authorities(user.getAuthorities())//
              .accountExpired(false)//
              .accountLocked(false)//
              .credentialsExpired(false)//
              .disabled(false)//
              .build();
    }

    throw new UsernameNotFoundException("User '" + username + "' not found");

  }

}
