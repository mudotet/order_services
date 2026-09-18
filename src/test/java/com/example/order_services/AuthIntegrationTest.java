package com.example.order_services;

import com.example.order_services.entity.Cart;
import com.example.order_services.entity.Role;
import com.example.order_services.entity.User;
import com.example.order_services.entity.UserRole;
import com.example.order_services.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false"
})
@Transactional
class AuthIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy securityFilterChain;
    @Autowired PasswordEncoder passwords;
    @Autowired EntityManager entityManager;
    @Autowired CurrentUserService currentUserService;

    MockMvc mvc;
    Map<String, User> accounts;
    Map<String, Role> roles;
    Cart customerCart;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
        accounts = new HashMap<>();
        roles = new HashMap<>();
        String hash = passwords.encode("password");
        for (String name : List.of("USER", "ADMIN", "SHIPPER")) {
            Role role = Role.builder().roleName(name).build();
            entityManager.persist(role);
            roles.put(name, role);
            User user = User.builder().userName("Shared display name").email(name.toLowerCase() + "@example.com")
                    .password(hash).build();
            entityManager.persist(user);
            entityManager.persist(UserRole.builder().user(user).role(role).build());
            accounts.put(name, user);
        }
        customerCart = Cart.builder().user(accounts.get("USER")).build();
        entityManager.persist(customerCart);
        entityManager.flush();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsInAllThreeRolesAndPersistsSessionWithoutTrustingClientRoles() throws Exception {
        for (String role : List.of("USER", "ADMIN", "SHIPPER")) {
            User user = accounts.get(role);
            MockHttpSession session = new MockHttpSession();
            String originalSessionId = session.getId();
            mvc.perform(post("/api/auth/login").session(session).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + user.getEmail()
                                    + "\",\"password\":\"password\",\"roles\":[\"ADMIN\"]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userId").value(user.getId()))
                    .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.data.roles", containsInAnyOrder(role)))
                    .andExpect(jsonPath("$.data.password").doesNotExist())
                    .andExpect(cookie().doesNotExist("XSRF-TOKEN"));
            assertThat(session.getId()).isNotEqualTo(originalSessionId);
            SecurityContext saved = (SecurityContext) session.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertThat(saved.getAuthentication().getName()).isEqualTo(user.getEmail());
            assertThat(saved.getAuthentication().getCredentials()).isNull();
            SecurityContextHolder.setContext(saved);
            assertThat(currentUserService.getCurrentUser().getId()).isEqualTo(user.getId());
            SecurityContextHolder.clearContext();
            var cart = mvc.perform(get("/api/carts").session(session))
                    .andExpect(status().is(role.equals("USER") ? 200 : 403));
            if (role.equals("USER")) {
                cart.andExpect(jsonPath("$.data.cartId").value(customerCart.getId()));
            }
            mvc.perform(get("/api/orders/returns").session(session))
                    .andExpect(status().is(role.equals("ADMIN") ? 200 : 403));
            if (role.equals("SHIPPER")) {
                mvc.perform(patch("/api/orders/tracking/missing/state").session(session)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"state\":\"SHIPPING\"}"))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void rejectsBadCredentialsDeletedAccountsAndInactiveOrUnsupportedRoles() throws Exception {
        User customer = accounts.get("USER");
        for (String email : List.of(customer.getEmail(), "missing@example.com")) {
            MockHttpSession session = new MockHttpSession();
            mvc.perform(login(email, "wrong-password").session(session))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
            mvc.perform(get("/api/carts").session(session)).andExpect(status().isUnauthorized());
        }
        mvc.perform(login(customer.getEmail(), "密".repeat(30))).andExpect(status().isUnauthorized());
        customer.setDeleted(true);
        entityManager.flush();
        mvc.perform(login(customer.getEmail(), "password")).andExpect(status().isUnauthorized());
        customer.setDeleted(false);
        roles.get("USER").setDeleted(true);
        entityManager.flush();
        mvc.perform(login(customer.getEmail(), "password")).andExpect(status().isUnauthorized());
        roles.get("USER").setDeleted(false);
        UserRole assignment = entityManager.find(UserRole.class,
                new UserRole.UserRoleId(customer.getId(), roles.get("USER").getId()));
        assignment.setDeleted(true);
        Role unsupported = Role.builder().roleName("SUPPORT").build();
        entityManager.persist(unsupported);
        entityManager.persist(UserRole.builder().user(customer).role(unsupported).build());
        entityManager.flush();
        mvc.perform(login(customer.getEmail(), "password")).andExpect(status().isUnauthorized());
    }

    @Test
    void validatesLoginWhileReturningAllActiveRoles() throws Exception {
        mvc.perform(login("not-an-email", "password")).andExpect(status().isBadRequest());
        mvc.perform(login("user@example.com", "")).andExpect(status().isBadRequest());
        accounts.get("USER").setUserName("legacy-login");
        entityManager.flush();
        mvc.perform(get("/api/carts").header(HttpHeaders.AUTHORIZATION, "Basic "
                        + Base64.getEncoder().encodeToString("legacy-login:password".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isUnauthorized());
        entityManager.persist(UserRole.builder().user(accounts.get("USER")).role(roles.get("SHIPPER")).build());
        entityManager.flush();
        mvc.perform(login("user@example.com", "password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder("USER", "SHIPPER")));
    }

    private MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }
}
