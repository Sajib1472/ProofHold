package com.proofhold.auth;

import com.proofhold.claim.ClaimController;
import com.proofhold.config.SecurityConfig;
import com.proofhold.domain.Role;
import com.proofhold.item.ItemController;
import com.proofhold.item.ItemService;
import com.proofhold.item.StaffItemResponse;
import com.proofhold.web.ProblemAccessDeniedHandler;
import com.proofhold.web.ProblemAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

    @MockBean
    ItemService itemService;

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
        when(itemService.create(any(), any()))
                .thenReturn(new StaffItemResponse(
                        18L, 1L, "Library Front Desk", null, null, null, 0, 0,
                        null, null, null, null, null, null, java.util.List.of()));
        mvc.perform(post("/v1/items")
                        .header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "locationId": 1,
                                  "category": "WALLET",
                                  "foundAt": "2026-10-05T14:00:00Z",
                                  "holdUntil": "2026-11-05T14:00:00Z",
                                  "whereFound": "2nd floor",
                                  "fullDescription": "black leather wallet",
                                  "challenges": [
                                    {"prompt": "What initials are inside?", "expectedAnswer": "JS"},
                                    {"prompt": "About how many cards?", "expectedAnswer": "8"}
                                  ]
                                }
                                """))
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
