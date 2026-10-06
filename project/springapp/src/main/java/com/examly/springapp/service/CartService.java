package com.examly.springapp.service;

import com.examly.springapp.model.Cart;
import java.util.List;

/**
 * Business operations on shopping carts.
 *
 * @author Shoryan
 */
public interface CartService {
    Cart addCart(Cart cart);
    Cart updateCart(Long cartId, Cart cart);
    Cart removeCourse(Long cartId, Long courseId);
    Cart removeAllCourses(Long cartId);
    Cart getCartById(Long cartId);
    Cart getCartByUserId(Long userId);
    Cart getCartByCustomerId(Long customerId);
    List<Cart> getAllCarts();
}
