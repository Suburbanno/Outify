package cc.tomko.outify.core.auth;

/**
 * Used when handling OAuth login responses.
 */
public final class TokenResponseDto {
    public final String accessToken;
    public final String refreshToken;
    public final long expiresAt; // duration in nanos
    public final String error;

    public TokenResponseDto(String accessToken, String refreshToken, long expiresAt, String error) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.error = error;
    }

    public boolean isSuccess(){
        return this.error == null;
    }
}
