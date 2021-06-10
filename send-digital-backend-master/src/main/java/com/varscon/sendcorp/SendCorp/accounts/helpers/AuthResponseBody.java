package com.varscon.sendcorp.SendCorp.accounts.helpers;

import com.varscon.sendcorp.SendCorp.accounts.models.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AuthResponseBody {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dob;
    private GenderModel gender;
    private Boolean hasKyc;
    private Boolean hasPin;
    private UserAddressModel address;
    private UserRoleModel appUserRole;
    private Status status;
    private boolean enabled;
    private boolean locked;

    public AuthResponseBody getProfile(UserModel user) {
        return new AuthResponseBody(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getDob(),
                user.getGenderModel(),
                user.getHasKyc(),
                user.getHasPin(),
                user.getAddress(),
                user.getAppUserRole(),
                user.getStatus(),
                user.getEnabled(),
                user.getLocked()
        );
    }

}
