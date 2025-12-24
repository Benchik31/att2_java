package ru.coursework.javasems.security;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AppUserService appUserService;

    public AuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                               BindingResult bindingResult) {
        if (!java.util.Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "match", "Passwords do not match");
        }
        if (form.getUsername() != null && appUserService.usernameExists(form.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Username already exists");
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }
        appUserService.registerUser(form.getUsername(), form.getPassword());
        return "redirect:/login?registered";
    }
}
