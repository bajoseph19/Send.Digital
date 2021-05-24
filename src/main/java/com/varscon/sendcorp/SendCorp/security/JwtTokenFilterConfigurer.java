package com.varscon.sendcorp.SendCorp.security;

import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenFilterConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {
  private final JwtTokenFilter customFilter;

  public JwtTokenFilterConfigurer(JwtTokenProvider jwtTokenProvider, JwtTokenFilter customFilter) {
    this.customFilter = customFilter;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
  }

}
