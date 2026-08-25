package com.cyz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/index")
    public String index() {
        return "api.html";
    }

    @GetMapping("/login")
    public String login() {
        return "login.html";
    }
}
