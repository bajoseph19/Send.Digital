package com.varscon.sendcorp.SendCorp.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import com.varscon.sendcorp.SendCorp.exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

  /**
   * THIS IS NOT A SECURE PRACTICE! For simplicity, we are storing a static key here. Ideally, in a
   * microservices environment, this key would be kept on a config-server.
   */
  @Value("${security.jwt.token.secret-key:secret-key}")
  private String secretKey;

  @Value("${security.jwt.token.expire-length:3600000}")
  private long validityInMilliseconds; // 1h

  @Autowired
  private AppUserDetails appUserDetails;

  private Algorithm algorithm;
  private JWTVerifier verifier;
  private JWTCreator.Builder builder;

  @PostConstruct
  protected void init() {
    secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    algorithm = Algorithm.HMAC256(secretKey);
    verifier = JWT.require(algorithm)
            .withIssuer("sendcorp")
            .build();
    builder = JWT.create()
            .withIssuer("sendcorp");
  }

  public String createUserToken(UserModel profile) {
    try {
      return
              builder
                      .withSubject(profile.getEmail())
                      .withClaim("payload", Map.of("id",profile.getId(), "email", profile.getEmail()))
                      .withExpiresAt(new Date(new Date().getTime() + validityInMilliseconds))
                      .sign(algorithm);
    } catch (JWTCreationException exception){
      //Invalid Signing configuration / Couldn't convert Claims.
      throw exception;
    }
  }


  public Authentication getAuthentication(String token) {
    UserDetails userDetails = appUserDetails.loadUserByUsername(getUsername(token));
    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
  }

  public String getUsername(String token) {
    DecodedJWT jwt = JWT.decode(token);
    return jwt.getSubject();
//    return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
  }

  public String resolveToken(HttpServletRequest req) {
    String bearerToken = req.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  public boolean validateToken(String token) {
    try {
      verifier.verify(token);
      return true;
    } catch (JWTVerificationException | IllegalArgumentException e) {
//      System.out.println("expired token");
      throw new CustomException("Expired or invalid JWT token", HttpStatus.BAD_REQUEST);
    }
  }

}
