package vn.edu.aros.aroscore.service;

public interface TurnstileService {

    /**
     * Verifies a Turnstile captcha token with Cloudflare.
     *
     * @param captchaToken token from the FE widget
     * @param remoteIp     optional client IP
     * @throws IllegalArgumentException if verification fails
     */
    void verifyOrThrow(String captchaToken, String remoteIp);
}
