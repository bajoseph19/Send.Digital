package com.varscon.sendcorp.SendCorp.accounts.models;

import lombok.*;

@EqualsAndHashCode()
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserKYCModel {
    private String governmentValidId;
    private String displayPicture;
    private String proofOfAddress;
}
