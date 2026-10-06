package com.examly.springapp.controller;

import com.examly.springapp.dto.InstructorDtos.*;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.InstructorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** The admin's view of instructor applications (the /api/admin/** rule already limits this to ADMIN). */
@RestController
@RequestMapping("/api/admin/instructors")
@Tag(name = "Instructor approvals", description = "Admin only: list, approve or reject instructor applications")
public class AdminInstructorController {
    private final InstructorService instructors;
    private final AccessService access;

    @Autowired
    public AdminInstructorController(InstructorService instructors, AccessService access) {
        this.instructors = instructors;
        this.access = access;
    }

    @Operation(summary = "List applications", description = "Optional ?status=PENDING|APPROVED|REJECTED|ALL")
    @GetMapping
    public ResponseEntity<List<InstructorView>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(instructors.list(status));
    }

    @Operation(summary = "Approve an instructor")
    @PostMapping("/{userId}/approve")
    public ResponseEntity<InstructorView> approve(@PathVariable Long userId) {
        return ResponseEntity.ok(instructors.approve(userId, access.currentEmail()));
    }

    @Operation(summary = "Reject an instructor", description = "Body: {reason} (5 to 500 characters, shown to the instructor).")
    @PostMapping("/{userId}/reject")
    public ResponseEntity<InstructorView> reject(@PathVariable Long userId, @RequestBody RejectRequest body) {
        return ResponseEntity.ok(instructors.reject(userId, body == null ? null : body.reason(), access.currentEmail()));
    }
}
