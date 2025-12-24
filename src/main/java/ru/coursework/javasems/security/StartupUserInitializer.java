package ru.coursework.javasems.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupUserInitializer implements CommandLineRunner {

    private final AppUserService appUserService;

    @Value("${app.security.admin.username:admin}")
    private String adminUsername;

    @Value("${app.security.admin.password:admin123}")
    private String adminPassword;

    public StartupUserInitializer(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public void run(String... args) {
        appUserService.createAdminIfMissing(adminUsername, adminPassword);
    }
}
