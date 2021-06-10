package com.varscon.sendcorp.SendCorp.accounts.endpoints;

import com.varscon.sendcorp.SendCorp.accounts.helpers.*;
import com.varscon.sendcorp.SendCorp.accounts.models.UserKYCModel;
import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import com.varscon.sendcorp.SendCorp.accounts.services.AccountService;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin()
@RestController
@RequestMapping(path = "api/v1/accounts/")
@AllArgsConstructor
@Api(description = "Manage User & Admin accounts", tags = "accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/user/signup")
    public ResponseEntity<AuthResponse<?>> register(@RequestBody RegistrationRequest request) {
        final AuthResponse<AuthResponseBody> data = accountService.register(request);
        return  ResponseEntity.ok(data);
    }

    @PostMapping("/user/login")
    public ResponseEntity<AuthResponse<?>> userLogin(@RequestBody LoginRequest request){
        final AuthResponse<AuthResponseBody> data = accountService.userLogin(request);
        return ResponseEntity.ok(data);
    }

    @PutMapping("/user/update")
    public ResponseEntity<?> accountUpdate(@RequestAttribute(value = "id") String id, @RequestBody UpdateRequest request){
        return ResponseEntity.ok(accountService.updateUser(id, request));
    }

    @PutMapping("/user/transaction-pin")
    public ResponseEntity<?> setTransactionPin(@RequestAttribute(value = "id") String id, @RequestBody String transactionPin){
        return ResponseEntity.ok(accountService.setTransactionPin(id, transactionPin));
    }

    @PutMapping("/user/kyc")
    public ResponseEntity<?> setKyc(@RequestAttribute(value = "id") String id, @RequestBody UserKYCModel userKYCModel){
        return ResponseEntity.ok(accountService.setKYC(id, userKYCModel));
    }

    @GetMapping(path = "confirm")
    public String confirm(@RequestParam("token") String token) {
        return accountService.confirmToken(token);
    }

    @GetMapping(path = "resend-verification-email")
    public ResponseEntity<?> resendConfirmationEmail(@RequestAttribute("email") String email) {
        accountService.resendConfirmationEmail(email);
        return ResponseEntity.ok("You should get an email if user still exists in our database");
    }

    @GetMapping("/user")
    public ResponseEntity<?> getAccount(@RequestAttribute(value = "id") String id ){
        return ResponseEntity.ok(accountService.getUser(id));
    }

}
