package com.examly.springapp.controller;

import com.examly.springapp.model.Cart;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Shopping cart endpoints. Customers can only touch their own cart (checked with AccessService);
 * only ADMIN may list all carts. Ids in the URL must be positive (@Validated + @Min).
 *
 * @author Shoryan
 */
@RestController
@RequestMapping("/api/cart")
@Validated
@Tag(name = "Cart", description = "Shopping cart of a customer")
public class CartController {
    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final AccessService access;

    @Autowired
    public CartController(CartService cartService, AccessService access) {
        this.cartService = cartService;
        this.access = access;
    }

    /** Loads the cart and checks it belongs to the caller. */
    private Cart owned(Long cartId) {
        Cart cart = cartService.getCartById(cartId);
        access.requireCustomerAccess(cart.getCustomer().getCustomerId());
        return cart;
    }

    @Operation(summary = "Add courses to the cart (creates the cart if needed)", description = "CUSTOMER only. The cart always belongs to the logged-in customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cart created or updated"),
            @ApiResponse(responseCode = "400", description = "A course has no courseId"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not a customer"),
            @ApiResponse(responseCode = "404", description = "Customer profile or course not found")
    })
    @PostMapping
    public ResponseEntity<Cart> addCart(@RequestBody Cart cart) {
        log.trace("addCart called");
        cart.setCustomer(access.currentCustomer());
        Cart saved = cartService.addCart(cart);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Replace the courses of a cart", description = "CUSTOMER only, own cart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart updated"),
            @ApiResponse(responseCode = "400", description = "Invalid id or course"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your cart"),
            @ApiResponse(responseCode = "404", description = "Cart or course not found")
    })
    @PutMapping("/{cartId}")
    public ResponseEntity<Cart> updateCart(@PathVariable @Min(value = 1, message = "must be a positive number") Long cartId,
                                           @RequestBody Cart cart) {
        log.trace("updateCart called for cartId={}", cartId);
        owned(cartId);
        return ResponseEntity.status(HttpStatus.OK).body(cartService.updateCart(cartId, cart));
    }

    @Operation(summary = "Remove one course from a cart", description = "CUSTOMER only, own cart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course removed, updated cart returned"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your cart"),
            @ApiResponse(responseCode = "404", description = "Cart not found or course not in the cart")
    })
    @DeleteMapping("/{cartId}/course/{courseId}")
    public ResponseEntity<Cart> removeCourse(@PathVariable @Min(value = 1, message = "must be a positive number") Long cartId,
                                             @PathVariable @Min(value = 1, message = "must be a positive number") Long courseId) {
        log.trace("removeCourse called for cartId={}, courseId={}", cartId, courseId);
        owned(cartId);
        return ResponseEntity.status(HttpStatus.OK).body(cartService.removeCourse(cartId, courseId));
    }

    @Operation(summary = "Remove all courses from a cart", description = "CUSTOMER only, own cart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart emptied"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your cart"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @DeleteMapping("/{cartId}")
    public ResponseEntity<Cart> removeAllCourses(@PathVariable @Min(value = 1, message = "must be a positive number") Long cartId) {
        log.trace("removeAllCourses called for cartId={}", cartId);
        owned(cartId);
        return ResponseEntity.status(HttpStatus.OK).body(cartService.removeAllCourses(cartId));
    }

    @Operation(summary = "Get the cart of a user", description = "CUSTOMER only, own data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your data"),
            @ApiResponse(responseCode = "404", description = "No cart for this user")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<Cart> getCartByUserId(@PathVariable @Min(value = 1, message = "must be a positive number") Long userId) {
        log.trace("getCartByUserId called for userId={}", userId);
        access.requireUserAccess(userId);
        return ResponseEntity.status(HttpStatus.OK).body(cartService.getCartByUserId(userId));
    }

    @Operation(summary = "Get the cart of a customer", description = "CUSTOMER only, own data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your data"),
            @ApiResponse(responseCode = "404", description = "No cart for this customer")
    })
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Cart> getCartByCustomerId(@PathVariable @Min(value = 1, message = "must be a positive number") Long customerId) {
        log.trace("getCartByCustomerId called for customerId={}", customerId);
        access.requireCustomerAccess(customerId);
        return ResponseEntity.status(HttpStatus.OK).body(cartService.getCartByCustomerId(customerId));
    }

    @Operation(summary = "Get a cart by id", description = "CUSTOMER only, own cart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart found"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your cart"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    @GetMapping("/{cartId}")
    public ResponseEntity<Cart> getCartById(@PathVariable @Min(value = 1, message = "must be a positive number") Long cartId) {
        log.trace("getCartById called for cartId={}", cartId);
        return ResponseEntity.status(HttpStatus.OK).body(owned(cartId));
    }

    @Operation(summary = "List all carts", description = "ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of carts (may be empty)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @GetMapping
    public ResponseEntity<List<Cart>> getAllCarts() {
        log.trace("getAllCarts called");
        List<Cart> carts = cartService.getAllCarts();
        log.debug("getAllCarts: returning {} cart(s)", carts.size());
        return ResponseEntity.status(HttpStatus.OK).body(carts);
    }
}
