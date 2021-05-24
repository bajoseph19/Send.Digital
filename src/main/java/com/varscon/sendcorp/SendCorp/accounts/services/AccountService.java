package com.varscon.sendcorp.SendCorp.accounts.services;

import com.varscon.sendcorp.SendCorp.accounts.helpers.*;
import com.varscon.sendcorp.SendCorp.accounts.models.Status;
import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import com.varscon.sendcorp.SendCorp.accounts.models.UserRoleModel;
import com.varscon.sendcorp.SendCorp.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class AccountService {

    private final UserService appUserService;
    private final EmailValidator emailValidator;
    private final ConfirmationTokenService confirmationTokenService;


    public AuthResponse<UserModel> register(RegistrationRequest request) {
        boolean isValidEmail = emailValidator.
                test(request.getEmail());

        if (!isValidEmail) {
            throw new IllegalStateException("email not valid");
        }

        AuthResponse<UserModel> response = appUserService.signUpUser(
                new UserModel(
                        request.getFirstName(),
                        request.getLastName(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getDob(),
                        request.getPhone(),
                        request.getGender(),
                        request.getAddress(),
                        Status.VERIFICATION_PENDING,
                        UserRoleModel.USER

                )
        );

        return response;
    }

    public AuthResponse<UserModel> userLogin(LoginRequest request) {
        return appUserService.userLogin(request.getEmail(), request.getPassword());
    }

    public UserModel getUser(String userId) {
        return appUserService.getUserById(userId);
    }

    public UserModel updateUser(String userId, UpdateRequest user) {
        UserModel profile = getUser(userId);

//        profile.setUpdatedAt(LocalDateTime.now());

        profile = user.update(profile);
        return appUserService.updateUser(profile);
    }

    @Transactional
    public String confirmToken(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService
                .getToken(token)
                .orElseThrow(() ->
                        new IllegalStateException("token not found"));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new IllegalStateException("email already confirmed");
        }

        LocalDateTime expiredAt = confirmationToken.getExpiresAt();

        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("token expired");
        }


        confirmationTokenService.setConfirmedAt(confirmationToken);
        appUserService.enableAppUser(confirmationToken.getAppUser());
        return "confirmed";
    }

}
