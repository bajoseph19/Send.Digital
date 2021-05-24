package com.varscon.sendcorp.SendCorp.accounts.helpers;

import com.varscon.sendcorp.SendCorp.accounts.models.GenderModel;
import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class UpdateRequest {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final LocalDate dob;
    private final GenderModel gender;

    public UserModel update(UserModel profile) {
        if(this.firstName != null){
            profile.setFirstName(this.firstName);
        }if(this.lastName != null){
            profile.setLastName(this.lastName);
        }if(this.dob != null){
            profile.setDob(this.dob);
        }if(this.phone != null){
            profile.setPhone(this.phone);
        }if(this.gender != null){
            profile.setGenderModel(this.gender);
        }
        return profile;
    }
}
