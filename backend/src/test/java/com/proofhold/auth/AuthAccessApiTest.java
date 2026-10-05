package com.proofhold.auth;

import com.proofhold.claim.ClaimController;
import com.proofhold.config.SecurityConfig;
import com.proofhold.domain.Role;
import com.proofhold.item.ItemController;
import com.proofhold.web.ProblemAccessDeniedHandler;
import com.proofhold.web.ProblemAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ItemController.class, ClaimController.class})
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtService.class,
    ProblemAuthenticationEntryPoint.class,
    ProblemAccessDeniedHandler.class
})
@TestPropertySource(properties = {
    "proofhold.jwt.secret=proofhold-dev-secret-change-me-32b",
    "proofhold.jwt.issuer=proofhold",
    "proofhold.jwt.access-seconds=3600"
})
class AuthAccessApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwtService;

    @Test
    void createItemWithoutTokenIs401() throws Exception {
        mvc.perform(post("/v1/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void createItemAsClaimerIs403() throws Exception {
        mvc.perform(post("/v1/items").header("Authorization", bearer(Role.CLAIMER)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void createItemAsStaffIs201() throws Exception {
        mvc.perform(post("/v1/items")
                        .header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void decideClaimWithoutTokenIs401() throws Exception {
        mvc.perform(post("/v1/claims/1/decision"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void decideClaimAsClaimerIs403() throws Exception {
        mvc.perform(post("/v1/claims/1/decision").header("Authorization", bearer(Role.CLAIMER)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    private String bearer(Role role) {
        return "Bearer " + jwtService.issue(role == Role.STAFF ? 1L : 2L, role.name().toLowerCase() + "@proofhold.local", role);
    }
}
