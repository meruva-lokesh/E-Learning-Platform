package com.examly.springapp.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body of POST /api/login.
 *
 * @author Suriya
 */
public class LoginDTO {
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "Password is required")
    private String password;

    /** No-arg constructor required by Jackson. */
    public LoginDTO() {}

    /** All-fields constructor; chains to the no-arg constructor. */
    public LoginDTO(String email, String password) {
        this();
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
