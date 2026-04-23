package com.trecapps.falsehoods.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FalsehoodRouter {
    @Autowired
    ObjectMapper objectMapper;

    @Value("${trecapps.falsehoods.url}")
    String falsehoodsUrl;
    @Value("${trecapps.falsehoods.base-path}")
    String falsehoodsPath;
}
