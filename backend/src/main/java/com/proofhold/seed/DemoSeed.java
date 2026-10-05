package com.proofhold.seed;

import com.proofhold.domain.Role;
import com.proofhold.location.Location;
import com.proofhold.location.LocationRepository;
import com.proofhold.user.User;
import com.proofhold.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Component
@ConditionalOnProperty(name = "proofhold.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoSeed implements ApplicationRunner {

    private final LocationRepository locations;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String seedPassword;

    public DemoSeed(
            LocationRepository locations,
            UserRepository users,
            PasswordEncoder passwords,
            @Value("${proofhold.seed.password:proofhold}") String seedPassword) {
        this.locations = locations;
        this.users = users;
        this.passwords = passwords;
        this.seedPassword = seedPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (locations.findByName("Library Front Desk").isEmpty()) {
            Location desk = new Location();
            desk.setName("Library Front Desk");
            desk.setTimezone("America/New_York");
            desk.setOpenFrom(LocalTime.of(9, 0));
            desk.setOpenTo(LocalTime.of(17, 0));
            locations.save(desk);
        }
        seedUser("staff@proofhold.local".toLowerCase(), Role.STAFF);
        seedUser("alice@proofhold.local", Role.CLAIMER);
        seedUser("bob@proofhold.local", Role.CLAIMER);
    }

    private void seedUser(String email, Role role) {
        if (users.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(seedPassword));
        user.setRole(role);
        users.save(user);
    }
}
