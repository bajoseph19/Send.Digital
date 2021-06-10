package com.varscon.sendcorp.SendCorp.accounts.services;

import com.varscon.sendcorp.SendCorp.accounts.helpers.*;
import com.varscon.sendcorp.SendCorp.accounts.models.Status;
import com.varscon.sendcorp.SendCorp.accounts.models.UserKYCModel;
import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import com.varscon.sendcorp.SendCorp.accounts.models.UserRoleModel;
import com.varscon.sendcorp.SendCorp.exceptions.BadRequestException;
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


    public AuthResponse<AuthResponseBody> register(RegistrationRequest request) {
        boolean isValidEmail = emailValidator.
                test(request.getEmail());

        if (!isValidEmail) {
            throw new BadRequestException("email not valid");
        }

        AuthResponse<AuthResponseBody> response = appUserService.signUpUser(
                new UserModel(
                        request.getFirstName(),
                        request.getLastName(),
                        request.getEmail(),
                        request.getPassword(),
                        "",
                        false,
                        false,
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

    public AuthResponse<AuthResponseBody> userLogin(LoginRequest request) {
        return appUserService.userLogin(request.getEmail(), request.getPassword());
    }

    public AuthResponseBody getUser(String userId) {
        return new AuthResponseBody().getProfile(appUserService.getUserById(userId));
    }

    private UserModel _getUser(String userId) {
        return appUserService.getUserById(userId);
    }

    public AuthResponseBody updateUser(String userId, UpdateRequest user) {
        UserModel profile = _getUser(userId);

//        profile.setUpdatedAt(LocalDateTime.now());

        profile = user.update(profile);
        AuthResponseBody authResponseBody = new AuthResponseBody();
        return authResponseBody.getProfile(appUserService.updateUser(profile));
    }

    public SuccessResponse<?> setTransactionPin(String userId, String pin) {
        UserModel profile = _getUser(userId);

        return new SuccessResponse<>(appUserService.setTransactionPin(profile, pin), null);
    }

    public SuccessResponse<?> setKYC(String userId, UserKYCModel userKYCModel) {
        UserModel profile = _getUser(userId);

        return new SuccessResponse<>(appUserService.setKyc(profile, userKYCModel), null);
    }

    public void resendConfirmationEmail(String email) {
        appUserService.sendConfirmationEmail(email);
    }

    @Transactional
    public String confirmToken(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService
                .getToken(token)
                .orElseThrow(() ->
                        new BadRequestException("token not found"));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new BadRequestException("email already confirmed");
        }

        LocalDateTime expiredAt = confirmationToken.getExpiresAt();

        if (expiredAt.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("token expired");
        }


        confirmationTokenService.setConfirmedAt(confirmationToken);
        appUserService.enableAppUser(confirmationToken.getAppUser());
        return "confirmed";
    }

}
