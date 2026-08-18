package com.inshort.be.voice.stt;

import org.springframework.web.multipart.MultipartFile;

public interface SpeechTranscriptionClient {

  String transcribe(MultipartFile audio);
}
