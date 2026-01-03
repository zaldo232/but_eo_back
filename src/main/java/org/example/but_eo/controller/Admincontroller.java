package org.example.but_eo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@RequestMapping("/admin")
@Controller
public class Admincontroller {
    @GetMapping
    public String login(){
        return "index";
    }
}
