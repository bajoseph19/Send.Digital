package com.varscon.sendcorp.SendCorp.accounts.services;


import com.varscon.sendcorp.SendCorp.accounts.helpers.AuthResponse;
import com.varscon.sendcorp.SendCorp.accounts.helpers.AuthResponseBody;
import com.varscon.sendcorp.SendCorp.accounts.helpers.ConfirmationToken;
import com.varscon.sendcorp.SendCorp.accounts.models.UserKYCModel;
import com.varscon.sendcorp.SendCorp.accounts.repositories.UserRepository;
import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import com.varscon.sendcorp.SendCorp.exceptions.BadRequestException;
import com.varscon.sendcorp.SendCorp.exceptions.CustomException;
import com.varscon.sendcorp.SendCorp.notifications.EmailSender;
import com.varscon.sendcorp.SendCorp.notifications.EmailTemplates;
import com.varscon.sendcorp.SendCorp.notifications.MailModel;
import com.varscon.sendcorp.SendCorp.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService {

    private final static String USER_NOT_FOUND_MSG =
            "user with email %s not found";

    private final UserRepository appUserRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ConfirmationTokenService confirmationTokenService;
//    private final EmailSender emailSender;
    private final EmailSender emailSender;
    private final JwtTokenProvider jwtHelper;
    private final AuthenticationManager authenticationManager;

//    @Override
//    public UserDetails loadUserByUsername(String email)
//            throws UsernameNotFoundException {
//        return appUserRepository.findByEmail(email)
//                .orElseThrow(() ->
//                        new UsernameNotFoundException(
//                                String.format(USER_NOT_FOUND_MSG, email)));
//    }

    public AuthResponse<AuthResponseBody> signUpUser(UserModel appUser) {
        boolean userExists = appUserRepository
                .findByEmail(appUser.getEmail())
                .isPresent();

        boolean phoneExists = appUserRepository
                .findByPhone(appUser.getPhone())
                .isPresent();

        if (userExists) {
            // TODO check of attributes are the same and
            // TODO if email not confirmed send confirmation email.

            throw new BadRequestException("email already taken");
        }

        if (phoneExists) {
            // TODO check of attributes are the same and
            // TODO if email not confirmed send confirmation email.

            throw new BadRequestException("phone already taken");
        }

        String encodedPassword = bCryptPasswordEncoder
                .encode(appUser.getPassword());

        appUser.setPassword(encodedPassword);

        UserModel userModel = appUserRepository.save(appUser);

        sendConfirmationToken(appUser);

        AuthResponseBody authResponseBody = new AuthResponseBody();

        return new AuthResponse<>(authResponseBody.getProfile(userModel), jwtHelper.createUserToken(userModel));
    }

    public void sendConfirmationEmail(String email) {
        Optional<UserModel> user = appUserRepository.findByEmail(email);

        user.ifPresent(this::sendConfirmationToken);
    }

    private void sendConfirmationToken(UserModel appUser) {
        String token = UUID.randomUUID().toString();

        ConfirmationToken confirmationToken = new ConfirmationToken(
                token,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(15),
                appUser
        );

        confirmationTokenService.saveConfirmationToken(
                confirmationToken);

//        TODO: SEND EMAIL

        String mailLink = "https://send-corp.herokuapp.com/api/v1/accounts/confirm?token=" + token;
        String demoLink = "http://localhost:8080/api/v1/accounts/confirm?token=" + token;

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("firstName", appUser.getFirstName());
        model.put("link", mailLink);
        model.put("appName", "Send Digital");
        model.put("contact", "support@send.digital");
        model.put("codeDuration", "15 minutes");

        MailModel mailModel = new MailModel();
        mailModel.setFrom("support@send.digital");
        mailModel.setMailTo(appUser.getEmail());
        mailModel.setSubject("Send Digital | Activation Email");
        mailModel.setProps(model);
        mailModel.setTemplate(EmailTemplates.WELCOME_EMAIL);

        emailSender.sendEmail(mailModel);

    }

    public AuthResponse<AuthResponseBody> userLogin(String email, String password) {
        try{
            Optional<UserModel> userResult = appUserRepository.findByEmail(email);
            if(userResult.isEmpty()){
                throw new BadCredentialsException("Invalid email or password");
            }
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

            UserModel userModel = userResult.get();
            final AuthResponseBody authResponseBody = new AuthResponseBody();
            return new AuthResponse<>(authResponseBody.getProfile(userModel), jwtHelper.createUserToken(userModel));

        }catch (Exception e){
            if(e instanceof BadCredentialsException)
                throw new BadRequestException("Invalid email or password", e);

            else throw e;
        }

    }

    public UserModel getUserById(String userId) {
        Optional<UserModel> user = appUserRepository.findById(userId);

        if(user.isEmpty()) {
            throw new BadRequestException("No user account found with this id");
        }

        return user.get();
    }

    public UserModel updateUser(UserModel userModel) {
        return appUserRepository.save(userModel);
    }

    public String setTransactionPin(UserModel userModel, String pin) {

        String encodedPin = bCryptPasswordEncoder
                .encode(pin);

        userModel.setTransaction_pin(encodedPin);
        userModel.setHasPin(true);
        appUserRepository.save(userModel);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("firstName", userModel.getFirstName());
        model.put("link", "");
        model.put("activity", "Your transaction pin was just reset, if this was done without your consent, kindly click the link below to report this immediately, else take no action.");
        model.put("appName", "Send Digital");
        model.put("contact", "support@send.digital");
        model.put("codeDuration", "15 minutes");

        MailModel mailModel = new MailModel();
        mailModel.setFrom("support@send.digital");
        mailModel.setMailTo(userModel.getEmail());
        mailModel.setSubject("Send Digital | Activity Notice");
        mailModel.setProps(model);
        mailModel.setTemplate(EmailTemplates.ACTIVITY_NOTICE);

        emailSender.sendEmail(mailModel);

        return "New Pin set";
    }

    public String setKyc(UserModel userModel, UserKYCModel userKYCModel) {

        userModel.setUserKYCModel(userKYCModel);
        userModel.setHasKyc(true);
        appUserRepository.save(userModel);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("firstName", userModel.getFirstName());
        model.put("link", "");
        model.put("activity", "Your KYC details has been received and is being reviewed, if this was done without your consent, kindly click the link below to report this immediately, else take no action.");
        model.put("appName", "Send Digital");
        model.put("contact", "support@send.digital");
        model.put("codeDuration", "15 minutes");

        MailModel mailModel = new MailModel();
        mailModel.setFrom("support@send.digital");
        mailModel.setMailTo(userModel.getEmail());
        mailModel.setSubject("Send Digital | KYC Notice");
        mailModel.setProps(model);
        mailModel.setTemplate(EmailTemplates.ACTIVITY_NOTICE);

        emailSender.sendEmail(mailModel);

        return "KYC is being reviewed";
    }


    public void enableAppUser(UserModel user) {
        user.setEnabled(true);
        appUserRepository.save(user);
    }

    private String buildConfirmationEmail(String name, String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Confirm your email</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hi " + name + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Thank you for registering. Please click on the below link to activate your account: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">Activate Now</a> </p></blockquote>\n Link will expire in 15 minutes. <p>See you soon</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }

    private String buildPinEmail(String name) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Confirm your email</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hi " + name + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> A new transaction pin was set on your account: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + "link" + "\">Activate Now</a> </p></blockquote>\n Link will expire in 15 minutes. <p>See you soon</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }
}