package com.lbb.lmps.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${LMPS_BASE_URL:/api/lmps}")
public class Controller {

    private static final Logger logger = LogManager.getLogger(Controller.class);

    @GetMapping("/help-check")
    public String TestService(){
        System.out.println("******* deeplink Service working *******");
        logger.info("test");
        return  "******* deeplink Service working *******";
    }
}
