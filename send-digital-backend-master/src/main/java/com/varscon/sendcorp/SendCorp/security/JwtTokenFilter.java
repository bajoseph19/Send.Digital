package com.varscon.sendcorp.SendCorp.security;

import com.varscon.sendcorp.SendCorp.accounts.helpers.ErrorResponse;
import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import com.varscon.sendcorp.SendCorp.accounts.repositories.UserRepository;
import com.varscon.sendcorp.SendCorp.exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Service
// We should use OncePerRequestFilter since we are doing a database call, there is no point in doing this more than once
public class JwtTokenFilter extends OncePerRequestFilter {


  private UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;

  public JwtTokenFilter(@Autowired JwtTokenProvider jwtTokenProvider, @Autowired UserRepository userRepository) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.userRepository = userRepository;
  }


  @Override
  protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
    String token = jwtTokenProvider.resolveToken(httpServletRequest);
    try {

      //Try to jump cors
      httpServletResponse.addHeader("Access-Control-Allow-Origin", "*");
      httpServletResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH, HEAD");
      httpServletResponse.addHeader("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
      httpServletResponse.addHeader("Access-Control-Expose-Headers", "Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
      httpServletResponse.addHeader("Access-Control-Allow-Credentials", "true");
      httpServletResponse.addIntHeader("Access-Control-Max-Age", 10);

      if (token != null && jwtTokenProvider.validateToken(token)) {
        String userName = jwtTokenProvider.getUsername(token);
        Authentication auth = jwtTokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(auth);

                Optional<UserModel> optProfile = userRepository.findByEmail(userName);

                if(optProfile.isPresent()){
                  UserModel profile = optProfile.get();

                  httpServletRequest.setAttribute("id", profile.getId());
                  httpServletRequest.setAttribute("email", profile.getEmail());
                  httpServletRequest.setAttribute("userType", profile.getAppUserRole());
                }
      }
    } catch (CustomException ex) {
      //this is very important, since it guarantees the user is not authenticated at all
      SecurityContextHolder.clearContext();
//      httpServletResponse.sendError(ex.getHttpStatus().value(), ex.getMessage());
//      System.out.println("handling exception " + ex);
      httpServletResponse.setContentType("application/json");
      httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      httpServletResponse.getOutputStream().println(new ErrorResponse(ex.getMessage()).toString());

      return;
    }

    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }

}
