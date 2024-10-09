package com.netflix.netflix.dto;

import lombok.Builder;

@Builder
public record MailBody(String to, String subject, String text) {

}
