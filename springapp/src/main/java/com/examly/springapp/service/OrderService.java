package com.examly.springapp.service;

import com.examly.springapp.model.Orders;
import java.util.List;

/**
 * Business operations on orders.
 *
 * @author Tanvi
 */
public interface OrderService {
    Orders addOrder(Orders order);
    List<Orders> getAllOrders();
    Orders getOrderById(Long orderId);
    List<Orders> getOrdersByCustomerId(Long customerId);
    Orders deleteOrder(Long orderId);
}
