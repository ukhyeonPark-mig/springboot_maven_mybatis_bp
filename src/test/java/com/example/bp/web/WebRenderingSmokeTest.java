package com.example.bp.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.bp.config.SecurityConfig;
import com.example.bp.config.ThymeleafConfig;
import com.example.bp.config.WebConfig;
import com.example.bp.domain.Setting;
import com.example.bp.security.AuthSessionService;
import com.example.bp.security.SecurityPrincipal;
import com.example.bp.service.BrandingService;
import com.example.bp.service.MailService;
import com.example.bp.service.OtpService;
import com.example.bp.service.SettingService;
import com.example.bp.service.TurnstileService;
import com.example.bp.service.UserService;
import com.example.bp.support.AppProperties;
import com.example.bp.support.RateLimiterService;
import com.example.bp.web.admin.AdminDashboardController;
import com.example.bp.web.home.ContactController;
import com.example.bp.web.home.HomeController;
import com.example.bp.web.home.LegalController;
import com.example.bp.web.home.SigninController;
import com.example.bp.web.home.SitemapController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * DB-free rendering smoke test: drives the real Thymeleaf engine (layout dialect,
 * Spring Security dialect, fragments) for the public + admin layouts so template
 * wiring errors surface without a database. Service deps are mocked.
 */
@WebMvcTest(controllers = {HomeController.class, SigninController.class, AdminDashboardController.class,
        ContactController.class, LegalController.class, SitemapController.class})
@Import({SecurityConfig.class, ThymeleafConfig.class, WebConfig.class, GlobalModelAttributes.class,
        WebRenderingSmokeTest.TestBeans.class})
@EnableConfigurationProperties(AppProperties.class)
@TestPropertySource(properties = {"app.name=BP", "app.url=http://localhost:8080"})
class WebRenderingSmokeTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean SettingService settingService;
    @MockBean TurnstileService turnstileService;
    @MockBean RateLimiterService rateLimiterService;
    @MockBean OtpService otpService;
    @MockBean UserService userService;
    @MockBean AuthSessionService authSessionService;
    @MockBean MailService mailService;

    @BeforeEach
    void stubs() {
        Setting setting = new Setting();
        setting.setFooter("© 2026 BP");
        setting.setVersion("1.0.0");
        setting.setPrivacy("<p>개인정보 본문</p>");
        setting.setTerms("<p>이용약관 본문</p>");
        given(settingService.get()).willReturn(setting);
        given(turnstileService.enabled()).willReturn(false);
        given(turnstileService.siteKey()).willReturn(null);
    }

    @Test
    void landingRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("문의하기")));
    }

    @Test
    void signinRendersThreeInOneCard() throws Exception {
        mockMvc.perform(get("/signin"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("로그인")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("회원가입")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"auth-card\"")));
    }

    @Test
    void signinTabSwitchReturnsCardFragment() throws Exception {
        mockMvc.perform(get("/signin/tab").param("page", "Signup"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("이름")));
    }

    @Test
    void adminDashboardRedirectsWhenAnonymous() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/signin"));
    }

    @Test
    void adminLayoutRendersForAdminPrincipal() throws Exception {
        SecurityPrincipal admin = new SecurityPrincipal(
                1L, "admin@example.com", "관리자", "admin", null, "x",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        mockMvc.perform(get("/admin/dashboard")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                admin, null, admin.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("대시보드")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("관리자")));
    }

    @Test
    void contactRenders() throws Exception {
        mockMvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("문의하기")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("보내기")));
    }

    @Test
    void privacyRendersStoredHtml() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("개인정보 처리방침")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("개인정보 본문")));
    }

    @Test
    void termsRendersStoredHtml() throws Exception {
        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("서비스 이용약관")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("이용약관 본문")));
    }

    @Test
    void sitemapListsPublicRoutes() throws Exception {
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<urlset")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("http://localhost:8080/contact")));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        BrandingService brandingService() {
            return new BrandingService("storage/branding");
        }
    }
}
