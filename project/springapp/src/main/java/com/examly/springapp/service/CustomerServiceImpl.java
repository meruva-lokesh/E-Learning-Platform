package com.examly.springapp.service;

import com.examly.springapp.dto.CustomerUpdateDTO;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Customer;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.CustomerRepo;
import com.examly.springapp.repository.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer profile rules: one profile per user, profile edits by the owner.
 * Repository failures are wrapped into DatabaseOperationException.
 *
 * @author Meruva Lokesh
 */
@Service
public class CustomerServiceImpl implements CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepo customerRepo;
    private final UserRepo userRepo;

    public CustomerServiceImpl(CustomerRepo customerRepo, UserRepo userRepo) {
        this.customerRepo = customerRepo;
        this.userRepo = userRepo;
    }

    @Override
    public Customer addCustomer(Customer customer) {
        log.trace("addCustomer entered");
        if (customer.getUser() == null || customer.getUser().getUserId() == null) {
            throw new InvalidRequestException("A user id is required to create a customer");
        }
        Long userId = customer.getUser().getUserId();
        User user = DatabaseOperationException.guard("loading a user", () -> userRepo.findById(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        // Business rule: a user can have only one customer profile.
        if (DatabaseOperationException.guard("checking the profile", () -> customerRepo.findByUser_UserId(userId)).isPresent()) {
            throw new DuplicateResourceException("Customer details already exist for this user");
        }
        customer.setCustomerId(null);
        customer.setUser(user);
        try {
            Customer saved = customerRepo.save(customer);
            log.info("Customer profile {} created for user {}", saved.getCustomerId(), userId);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // two requests created the profile at the same moment
            throw new DuplicateResourceException("Customer details already exist for this user");
        } catch (DataAccessException e) {
            throw new DatabaseOperationException("Database error while saving a customer", e);
        }
    }

    @Override
    public Customer getCustomerById(Long customerId) {
        log.trace("getCustomerById entered for customerId={}", customerId);
        return DatabaseOperationException.guard("loading a customer", () -> customerRepo.findById(customerId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
    }

    @Override
    public Customer getCustomerByUserId(Long userId) {
        log.trace("getCustomerByUserId entered for userId={}", userId);
        return DatabaseOperationException.guard("loading a customer", () -> customerRepo.findByUser_UserId(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for user id " + userId));
    }

    @Override
    @Transactional
    public Customer updateCustomer(Long customerId, CustomerUpdateDTO update) {
        log.trace("updateCustomer entered for customerId={}", customerId);
        Customer customer = getCustomerById(customerId);   // 404 when the profile does not exist
        String name = update.getCustomerName().trim();
        if (name.length() < 3) {
            throw new InvalidRequestException("Customer name must be at least 3 characters");
        }
        customer.setCustomerName(name);
        customer.setInformation(update.getInformation().trim());
        // Business rule: the mobile number lives on the User account, so it is only touched when the client sent one.
        String mobile = update.getMobileNumber();
        if (mobile != null && !mobile.isBlank() && customer.getUser() != null) {
            User user = customer.getUser();
            user.setMobileNumber(mobile.trim());
            DatabaseOperationException.guard("updating the mobile number", () -> userRepo.save(user));
            log.debug("updateCustomer: mobile number changed for customer {}", customerId);
        }
        Customer saved = DatabaseOperationException.guard("updating a customer", () -> customerRepo.save(customer));
        log.info("Customer profile {} updated", customerId);
        return saved;
    }
}
