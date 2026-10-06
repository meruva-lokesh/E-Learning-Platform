package com.examly.springapp.repository;

import com.examly.springapp.model.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for invoices. */
@Repository
public interface InvoiceRepo extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByPaymentId(Long paymentId);
    List<Invoice> findByCustomerIdOrderByIdDesc(Long customerId);
}
