package com.examly.springapp.service;

import com.examly.springapp.dto.CustomerUpdateDTO;
import com.examly.springapp.model.Customer;

/**
 * Business operations on customer profiles.
 *
 * @author Meruva Lokesh
 */
public interface CustomerService {
    Customer addCustomer(Customer customer);
    Customer getCustomerById(Long customerId);
    Customer getCustomerByUserId(Long userId);

    /** Updates name and information (and optionally the mobile number of the linked user). */
    Customer updateCustomer(Long customerId, CustomerUpdateDTO update);
}
