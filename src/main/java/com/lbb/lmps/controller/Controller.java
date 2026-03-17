package com.lbb.lmps.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${baseurl}")
public class Controller {

    private static final Logger logger = LogManager.getLogger(Controller.class);

    @GetMapping("/test")
    public String TestService(){
        return  "******* deeplink Service working *******";
    }
}
