package com.example.modern;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private final DataSource dataSource;

    public GreetingController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/greet")
    public Map<String, String> greet(@RequestParam @NotBlank String name) {
        return Map.of("message", "Hello " + name);
    }
}
