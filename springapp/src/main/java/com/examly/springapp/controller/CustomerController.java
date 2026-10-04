package com.examly.springapp.controller;

import com.examly.springapp.dto.CustomerUpdateDTO;
import com.examly.springapp.model.Customer;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Customer profile endpoints. The profile is always linked to the logged-in user and
 * can only be edited by its owner; ADMIN may read any profile by customer id.
 *
 * @author Meruva Lokesh
 */
@RestController
@RequestMapping("/api/customer")
@Validated
@Tag(name = "Customers", description = "Customer profile of a logged-in user")
public class CustomerController {
    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final AccessService access;

    @Autowired
    public CustomerController(CustomerService customerService, AccessService access) {
        this.customerService = customerService;
        this.access = access;
    }

    @Operation(summary = "Create the customer profile", description = "CUSTOMER only. The profile is linked to the logged-in user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Profile created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not a customer"),
            @ApiResponse(responseCode = "409", description = "Profile already exists for this user")
    })
    @PostMapping
    public ResponseEntity<Customer> addCustomer(@Valid @RequestBody Customer customer) {
        log.trace("addCustomer called");
        // the customer always belongs to the logged-in user, whatever the request body says
        customer.setUser(access.currentUser());
        Customer saved = customerService.addCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Update the own customer profile",
            description = "CUSTOMER only, own profile. Body: customerName (3-100 chars), information (max 500), optional mobileNumber (10 digits, stored on the user).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated, updated customer returned"),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your profile"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PutMapping("/{customerId}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable @Min(value = 1, message = "must be a positive number") Long customerId,
            @Valid @RequestBody CustomerUpdateDTO update) {
        log.trace("updateCustomer called for customerId={}", customerId);
        access.requireCustomerAccess(customerId);   // ownership: a customer can only edit their own profile
        Customer updated = customerService.updateCustomer(customerId, update);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }

    @Operation(summary = "Get a customer by id", description = "ADMIN only (customers read their own profile with /api/customer/user/{userId}).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your data"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{customerId}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable @Min(value = 1, message = "must be a positive number") Long customerId) {
        log.trace("getCustomerById called for customerId={}", customerId);
        access.requireCustomerAccess(customerId);
        return ResponseEntity.status(HttpStatus.OK).body(customerService.getCustomerById(customerId));
    }

    @Operation(summary = "Get the customer profile of a user", description = "CUSTOMER only, own data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your data"),
            @ApiResponse(responseCode = "404", description = "No profile for this user")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<Customer> getCustomerByUserId(@PathVariable @Min(value = 1, message = "must be a positive number") Long userId) {
        log.trace("getCustomerByUserId called for userId={}", userId);
        access.requireUserAccess(userId);
        return ResponseEntity.status(HttpStatus.OK).body(customerService.getCustomerByUserId(userId));
    }
}
