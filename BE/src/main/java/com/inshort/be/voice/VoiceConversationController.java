package com.inshort.be.voice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice-conversations")
@Tag(name = "음성 대화", description = "Redis에 30분 동안 보관되는 음성 대화 API")
public class VoiceConversationController {

  private final VoiceConversationService conversationService;

  public VoiceConversationController(VoiceConversationService conversationService) {
    this.conversationService = conversationService;
  }

  @PostMapping
  @Operation(summary = "대화 시작", description = "새 대화를 생성하고 Redis TTL을 1,800초로 설정합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "대화 생성 성공",
      content = @Content(schema = @Schema(implementation = VoiceConversation.class)))
  public ResponseEntity<VoiceConversation> start() {
    VoiceConversation conversation = conversationService.start();
    return ResponseEntity.created(
            URI.create("/api/voice-conversations/" + conversation.conversationId()))
        .body(conversation);
  }

  @PostMapping("/{conversationId}/messages")
  @Operation(summary = "메시지 추가", description = "메시지를 추가하고 Redis TTL을 1,800초로 갱신합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "메시지 추가 성공"),
    @ApiResponse(responseCode = "400", description = "메시지 검증 실패"),
    @ApiResponse(responseCode = "404", description = "대화가 없거나 만료됨")
  })
  public VoiceConversation appendMessage(
      @Parameter(description = "대화 UUID", required = true) @PathVariable UUID conversationId,
      @Valid @RequestBody MessageRequest request) {
    return conversationService.appendMessage(conversationId, request.content());
  }

  @GetMapping("/{conversationId}")
  @Operation(summary = "대화 조회", description = "대화 메시지와 남은 Redis TTL을 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "대화 조회 성공"),
    @ApiResponse(responseCode = "404", description = "대화가 없거나 만료됨")
  })
  public VoiceConversation find(
      @Parameter(description = "대화 UUID", required = true) @PathVariable UUID conversationId) {
    return conversationService.find(conversationId);
  }

  @DeleteMapping("/{conversationId}")
  @Operation(summary = "대화 종료", description = "Redis에서 대화 데이터를 즉시 삭제합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "대화 삭제 성공"),
    @ApiResponse(responseCode = "404", description = "대화가 없거나 만료됨")
  })
  public ResponseEntity<Void> end(
      @Parameter(description = "대화 UUID", required = true) @PathVariable UUID conversationId) {
    conversationService.end(conversationId);
    return ResponseEntity.noContent().build();
  }

  public record MessageRequest(
      @Schema(description = "대화 메시지", example = "안녕하세요", maxLength = 4_000)
          @NotBlank
          @Size(max = 4_000)
          String content) {}
}
