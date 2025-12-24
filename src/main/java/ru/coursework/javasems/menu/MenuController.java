package ru.coursework.javasems.menu;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class MenuController {

    private final MenuItemRepository menuItemRepository;

    public MenuController(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @GetMapping("/")
    public String showMenu(Model model) {
        model.addAttribute("menuItem", new MenuItem());
        model.addAttribute("items", menuItemRepository.findAll(Sort.by("id")));
        return "menu";
    }

    @PostMapping("/items")
    public String addMenuItem(@Valid @ModelAttribute("menuItem") MenuItem menuItem,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("items", menuItemRepository.findAll(Sort.by("id")));
            return "menu";
        }
        menuItemRepository.save(menuItem);
        return "redirect:/";
    }

    @GetMapping("/items/{id}/edit")
    public String editMenuItem(@PathVariable Long id, Model model) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + id));
        model.addAttribute("item", item);
        return "edit";
    }

    @PostMapping("/items/{id}")
    public String updateMenuItem(@PathVariable Long id,
                                 @Valid @ModelAttribute("item") MenuItem item,
                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            item.setId(id);
            return "edit";
        }
        item.setId(id);
        menuItemRepository.save(item);
        return "redirect:/";
    }

    @PostMapping("/items/{id}/delete")
    public String deleteMenuItem(@PathVariable Long id) {
        menuItemRepository.deleteById(id);
        return "redirect:/";
    }
}
