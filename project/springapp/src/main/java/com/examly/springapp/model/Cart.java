package com.examly.springapp.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A customer's shopping cart: the courses they intend to buy and the running total.
 *
 * @author Shoryan
 */
@Entity
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "cart_course",
            joinColumns = @JoinColumn(name = "cart_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private List<Course> courses = new ArrayList<>();

    private Double totalAmount = 0.0;

    /** No-arg constructor required by JPA and Jackson. */
    public Cart() {}

    /** Convenience constructor for a new cart (no id yet); chains to the all-fields constructor. */
    public Cart(Customer customer, List<Course> courses, Double totalAmount) {
        this(null, customer, courses, totalAmount);
    }

    /** All-fields constructor; chains to the no-arg constructor so the defaults are always initialised. */
    public Cart(Long cartId, Customer customer, List<Course> courses, Double totalAmount) {
        this();
        this.cartId = cartId;
        this.customer = customer;
        this.courses = courses == null ? new ArrayList<>() : courses;
        this.totalAmount = totalAmount;
    }

    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) { this.courses = courses; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
}
