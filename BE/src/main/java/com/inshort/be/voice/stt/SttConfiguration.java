package com.inshort.be.voice.stt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GroqSttProperties.class)
public class SttConfiguration {}
