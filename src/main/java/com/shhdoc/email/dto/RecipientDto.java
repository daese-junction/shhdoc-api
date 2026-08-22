package com.shhdoc.email.dto;

import com.shhdoc.email.EmailRecipient;
import com.shhdoc.email.RecipientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecipientDto(
        @Schema(description = "수신자 주소. 사외 주소도 가능하다.", example = "partner@example.com")
        @Email @NotBlank String address,

        @Schema(description = "TO | CC | BCC", example = "TO")
        @NotNull RecipientType type) {

    public static RecipientDto from(EmailRecipient recipient) {
        return new RecipientDto(recipient.getAddress(), recipient.getType());
    }
}
