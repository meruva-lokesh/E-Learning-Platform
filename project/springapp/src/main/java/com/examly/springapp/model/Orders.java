package com.examly.springapp.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A placed order: the purchased courses, the price computed on the server and the customer.
 *
 * @author Tanvi
 */
@Entity
@Table(name = "orders")
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private Double orderPrice;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "orders_course",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private List<Course> courses = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** No-arg constructor required by JPA and Jackson. */
    public Orders() {}

    /** Convenience constructor for a new order (no id yet); chains to the all-fields constructor. */
    public Orders(Double orderPrice, List<Course> courses, Customer customer) {
        this(null, orderPrice, courses, customer);
    }

    /** All-fields constructor; chains to the no-arg constructor so the defaults are always initialised. */
    public Orders(Long orderId, Double orderPrice, List<Course> courses, Customer customer) {
        this();
        this.orderId = orderId;
        this.orderPrice = orderPrice;
        this.courses = courses == null ? new ArrayList<>() : courses;
        this.customer = customer;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Double getOrderPrice() { return orderPrice; }
    public void setOrderPrice(Double orderPrice) { this.orderPrice = orderPrice; }
    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) { this.courses = courses; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
}
