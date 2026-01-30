package cmc.delta.domain.user.adapter.in.dto.request;

import java.time.LocalDate;

public record UserOnboardingRequest(
    String nickname,
    LocalDate birthDate,
    boolean termsAgreed) {
}
