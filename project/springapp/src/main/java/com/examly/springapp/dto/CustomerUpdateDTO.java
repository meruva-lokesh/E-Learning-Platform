package com.examly.springapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body of PUT /api/customer/{customerId}. The mobile number is optional and, when sent,
 * is stored on the linked User account.
 *
 * @author Meruva Lokesh
 */
public class CustomerUpdateDTO {
    @NotBlank(message = "Customer name is required")
    @Size(min = 3, max = 100, message = "Customer name must be 3 to 100 characters")
    private String customerName;

    @NotBlank(message = "Information is required")
    @Size(max = 500, message = "Information is too long (maximum 500 characters)")
    private String information;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    public CustomerUpdateDTO() {}

    /** Name and information only (no mobile number change); chains to the all-fields constructor. */
    public CustomerUpdateDTO(String customerName, String information) {
        this(customerName, information, null);
    }

    /** All-fields constructor; chains to the no-arg constructor. */
    public CustomerUpdateDTO(String customerName, String information, String mobileNumber) {
        this();
        this.customerName = customerName;
        this.information = information;
        this.mobileNumber = mobileNumber;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getInformation() { return information; }
    public void setInformation(String information) { this.information = information; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
}
