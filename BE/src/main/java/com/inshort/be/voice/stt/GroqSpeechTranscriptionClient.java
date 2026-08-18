package com.inshort.be.voice.stt;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GroqSpeechTranscriptionClient implements SpeechTranscriptionClient {

  private final RestClient restClient;
  private final GroqSttProperties properties;

  public GroqSpeechTranscriptionClient(RestClient.Builder builder, GroqSttProperties properties) {
    this.restClient = builder.baseUrl(properties.baseUrl()).build();
    this.properties = properties;
  }

  @Override
  public String transcribe(MultipartFile audio) {
    if (properties.apiKey() == null || properties.apiKey().isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "STT is not configured");
    }

    try {
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", audioResource(audio));
      body.add("model", properties.model());
      body.add("language", properties.language());
      body.add("response_format", "json");
      body.add("temperature", "0");

      GroqTranscriptionResponse response =
          restClient
              .post()
              .uri("/audio/transcriptions")
              .header("Authorization", "Bearer " + properties.apiKey())
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(body)
              .retrieve()
              .body(GroqTranscriptionResponse.class);

      if (response == null || response.text() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "STT returned no transcript");
      }
      return response.text();
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (RestClientException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "STT request failed", exception);
    } catch (Exception exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Audio could not be read", exception);
    }
  }

  private ByteArrayResource audioResource(MultipartFile audio) throws Exception {
    byte[] bytes = audio.getBytes();
    String filename = audio.getOriginalFilename();
    return new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return filename == null || filename.isBlank() ? "speech.webm" : filename;
      }
    };
  }

  private record GroqTranscriptionResponse(String text) {}
}
