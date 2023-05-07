package com.studyolle.studyollejaden.account;

import com.studyolle.studyollejaden.domain.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.BDDMockito.then;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    private JavaMailSender javaMailSender;

    @DisplayName("회원 가입 화면 보이는지 테스트")
    @Test
    void signupForm() throws Exception {
        mockMvc.perform(get("/sign-up"))
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"));
    }

    @DisplayName("회원 가입 처리 - 입력값 오류")
    @Test
    void signUpSubmit_with_wrong_input() throws Exception {
        mockMvc.perform(
                        post("/sign-up")
                                .param("nickname", "user")
                                .param("email", "email..")
                                .param("password", "12345")
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"));
    }

    @DisplayName("회원 가입 처리 - 입력값 정상")
    @Test
    void signUpSubmit_with_correct_input() throws Exception {
        mockMvc.perform(
                        post("/sign-up")
                                .param("nickname", "user")
                                .param("email", "user@email.com")
                                .param("password", "12345678")
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));

        Account account = accountRepository.findByEmail("user@email.com");
        assertNotNull(account);
        assertNotEquals(account.getPassword(), "12345678");
        assertNotNull(account.getEmailCheckToken());
        then(javaMailSender).should().send(any(SimpleMailMessage.class));
    }

    @DisplayName("인증 메일 확인 - 입력값 오류")
    @Test
    void checkEmailToken_with_wrong_input() throws Exception {
        mockMvc.perform(
                        get("/check-email-token")
                                .param("token", "sadcmsadcmo")
                                .param("email", "user@email.com")
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("account/checked-email"));
    }

    @DisplayName("인증 메일 확인 - 입력값 정상")
    @Test
    @Transactional
    void checkEmailToken_with_correct_input() throws Exception {
        Account account = Account.builder()
                .email("user@email.com")
                .password("12345678")
                .nickname("username")
                .build();
        Account newAccount = accountRepository.save(account);
        newAccount.generateEmailCheckToken();

        mockMvc.perform(
                        get("/check-email-token")
                                .param("token", newAccount.getEmailCheckToken())
                                .param("email", newAccount.getEmail())
                )
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("nickname"))
                .andExpect(model().attributeExists("numberOfUser"))
                .andExpect(view().name("account/checked-email"));
    }
}