package com.davigama.assessflow.identity.infrastructure;

import com.davigama.assessflow.identity.domain.AccountType;
import com.davigama.assessflow.identity.domain.User;
import java.time.Clock;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local & !production")
@ConditionalOnProperty(name = "APP_LOCAL_DEMO_ACCOUNTS", havingValue = "true")
public class LocalDemoAccounts implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final Environment env;
    private final Clock clock;

    public LocalDemoAccounts(UserRepository users, PasswordEncoder passwords, Environment env, Clock clock) {
        this.users = users;
        this.passwords = passwords;
        this.env = env;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        create("TEST_TEACHER", AccountType.TEACHER, "Professor de teste");
        create("TEST_STUDENT", AccountType.STUDENT, "Aluno de teste");
    }

    private void create(String prefix, AccountType type, String name) {
        String email = env.getRequiredProperty(prefix + "_EMAIL").trim().toLowerCase(Locale.ROOT);
        String password = env.getRequiredProperty(prefix + "_PASSWORD");
        if (password.length() < 12) throw new IllegalStateException(prefix + "_PASSWORD must have at least 12 characters.");
        if (!users.existsByEmail(email))
            users.save(new User(email, passwords.encode(password), name, type, clock.instant()));
    }
}
