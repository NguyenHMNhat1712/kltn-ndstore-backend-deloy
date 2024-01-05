package com.example.officepcstore.security.oauth;


import com.example.officepcstore.config.Constant;
import com.example.officepcstore.map.UserMap;
import com.example.officepcstore.models.enity.User;
import com.example.officepcstore.models.enums.AccountType;
import com.example.officepcstore.payload.response.LoginResponse;
import com.example.officepcstore.repository.UserRepository;
import com.example.officepcstore.security.jwt.JwtUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
@AllArgsConstructor
public class Success extends SavedRequestAwareAuthenticationSuccessHandler {
    private UserRepository userRepository;
    private JwtUtils jwtUtil;
    private UserMap userMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        AccountType provider = AccountType.valueOf(oauth2User.getOauth2ClientName().toUpperCase());

        Optional<User> user = userRepository.findUserByEmailAndStatusUser(oauth2User.getEmail(), Constant.USER_ACTIVE);
        if (user.isEmpty()) {
            String accessToken = processAddUser(oauth2User, provider);
            response.sendRedirect(generateRedirectURL(true, accessToken, provider, ""));
        } else {
            try {
                if (EnumUtils.isValidEnum(AccountType.class, user.get().getAccountType().name()) &&
                        !user.get().getAccountType().equals(AccountType.LOCAL) &&
                        provider.equals(user.get().getAccountType())) {
                    String accessToken = jwtUtil.generateTokenFromUserId(user.get());
                    response.sendRedirect(generateRedirectURL(true, accessToken, provider, ""));
                } else response.sendRedirect(generateRedirectURL(false, "",
                        user.get().getAccountType(), user.get().getEmail() + " already have an account with +"+
                                user.get().getAccountType() +" method"));
            } catch (NullPointerException e) {
                response.sendRedirect(generateRedirectURL(false, "",
                        user.get().getAccountType(), user.get().getEmail() + " already have an account with +"+
                                user.get().getAccountType() +" method"));
            }
        }
    }

    public String processAddUser(CustomOAuth2User oAuth2User, AccountType social) {
        User newUser = new User(oAuth2User.getName(), oAuth2User.getEmail(), "",
                "", 0, 0, 0, "unknown", Constant.ROLE_USER,
                oAuth2User.getProfilePicture(), Constant.USER_ACTIVE,social);
        userRepository.save(newUser);
        String accessToken = jwtUtil.generateTokenFromUserId(newUser);
        LoginResponse res = userMapper.toLoginRes(newUser);
        res.setAccessToken(accessToken);
        return accessToken;
    }

    public String generateRedirectURL(Boolean success, String token, AccountType social, String message) {
        logger.debug(message);
        String CLIENT_HOST_REDIRECT = "https://kltn-nd-store.vercel.app/oauth2/redirect?token=";
        return CLIENT_HOST_REDIRECT + token + "&success=" + success + "&social=" + social.toString();
    }
}
