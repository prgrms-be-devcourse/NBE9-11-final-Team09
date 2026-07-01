package com.back.team9.moyeota.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class MailTemplateContext {
    private String templateName; // 예: mail/min-reached
    private Map<String, Object> variables; // 템플릿에 담길 변수 맵
}
