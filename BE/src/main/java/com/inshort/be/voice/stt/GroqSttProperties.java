package com.inshort.be.voice.stt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "stt.groq")
public record GroqSttProperties(
    String apiKey, String baseUrl, String model, String language, DataSize maxFileSize) {}
