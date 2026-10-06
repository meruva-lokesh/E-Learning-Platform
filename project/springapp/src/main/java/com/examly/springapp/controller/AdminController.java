package com.examly.springapp.controller;

import com.examly.springapp.model.User;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin account management: every admin can list the admins, only the main admin (from application.properties) can add or remove them. Customers cannot become admins any other way.
 *
 * @author Suriya
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin management", description = "List, add and remove admin accounts (ADMIN only)")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final UserService userService;
    private final AccessService accessService;
    /** The main admin from application.properties: the only admin who may add or remove admins. */
    private final String mainAdminEmail;

    @Autowired
    public AdminController(UserService userService, AccessService accessService,
                           @Value("${app.admin.email}") String mainAdminEmail) {
        this.userService = userService;
        this.accessService = accessService;
        this.mainAdminEmail = mainAdminEmail.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isMainAdmin() {
        return accessService.currentEmail().equalsIgnoreCase(mainAdminEmail);
    }

    private void requireMainAdmin() {
        if (!isMainAdmin()) {
            log.warn("Admin management refused: caller is not the main admin");
            throw new AccessDeniedException("Only the main admin can add or remove admins");
        }
    }

    @Operation(summary = "Who am I?", description = "Tells the page whether the caller is the main admin (may add and remove admins).")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", accessService.currentEmail());
        body.put("mainAdmin", isMainAdmin());
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    @Operation(summary = "List the admin accounts")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admins returned"),
            @ApiResponse(responseCode = "401", description = "Not logged in"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @GetMapping
    public ResponseEntity<List<User>> list() {
        log.trace("list admins called");
        return ResponseEntity.status(HttpStatus.OK).body(userService.listAdmins());
    }

    @Operation(summary = "Create another admin")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Admin created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Only the main admin may do this"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody User user) {
        log.trace("create admin called");
        requireMainAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createAdmin(user));
    }

    @Operation(summary = "Remove an admin", description = "Main admin only. The main admin itself, yourself and the last admin cannot be removed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin removed"),
            @ApiResponse(responseCode = "400", description = "Not allowed (yourself, last admin, not an admin)"),
            @ApiResponse(responseCode = "404", description = "Admin not found")
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        log.trace("delete admin called");
        requireMainAdmin();
        userService.deleteAdmin(userId, accessService.currentEmail());
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}