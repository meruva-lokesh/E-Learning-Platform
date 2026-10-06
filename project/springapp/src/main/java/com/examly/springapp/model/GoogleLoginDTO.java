package com.examly.springapp.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of POST /api/auth/google. "credential" is the Google ID token that the
 * "Sign in with Google" button gives the browser.
 */
public class GoogleLoginDTO {
    @NotBlank(message = "Google credential is required")
    private String credential;

    /** "login" (default) only lets an already registered person in; "register" creates the account first. */
    private String mode = "login";

    /** No-arg constructor required by Jackson. */
    public GoogleLoginDTO() {}

    public String getCredential() { return credential; }
    public void setCredential(String credential) { this.credential = credential; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isRegister() { return "register".equalsIgnoreCase(mode); }
}
