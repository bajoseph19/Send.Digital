package com.varscon.sendcorp.SendCorp.accounts.controllers;

import com.varscon.sendcorp.SendCorp.accounts.helpers.AuthResponse;
import com.varscon.sendcorp.SendCorp.accounts.helpers.LoginRequest;
import com.varscon.sendcorp.SendCorp.accounts.helpers.RegistrationRequest;
import com.varscon.sendcorp.SendCorp.accounts.helpers.UpdateRequest;
import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import com.varscon.sendcorp.SendCorp.accounts.services.AccountService;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "api/v1/accounts/")
@AllArgsConstructor
@Api(description = "Manage User & Admin accounts", tags = "accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/user/signup")
    public ResponseEntity<AuthResponse<?>> register(@RequestBody RegistrationRequest request) {
        final AuthResponse<UserModel> data = accountService.register(request);
        return  ResponseEntity.ok(data);
    }

    @PostMapping("/user/login")
    public ResponseEntity<AuthResponse<?>> userLogin(@RequestBody LoginRequest request){
        final AuthResponse<UserModel> data = accountService.userLogin(request);
        return ResponseEntity.ok(data);
    }

    @PutMapping
    public ResponseEntity<?> accountUpdate(@RequestAttribute(value = "id") String id, @RequestBody UpdateRequest request){
        return ResponseEntity.ok(accountService.updateUser(id, request));
    }

    @GetMapping(path = "confirm")
    public String confirm(@RequestParam("token") String token) {
        return accountService.confirmToken(token);
    }

}
