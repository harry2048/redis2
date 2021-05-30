package com.baidu.redis2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
public class SessionController {

    @GetMapping("/springSession")
    public String springSession(HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(600);
        session.setAttribute("name","zhangsan");
        String id = session.getId();
        System.out.println("id = " + id);
        return "redis2";
    }
}
