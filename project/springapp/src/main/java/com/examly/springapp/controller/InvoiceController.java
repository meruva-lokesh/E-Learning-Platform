package com.examly.springapp.controller;

import com.examly.springapp.dto.PayDtos.InvoiceView;
import com.examly.springapp.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Invoices of paid orders. A customer sees only his own; an admin sees all. */
@RestController
@RequestMapping("/api/invoice")
@Tag(name = "Invoices", description = "Invoices for verified payments")
public class InvoiceController {
    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @Operation(summary = "My invoices, newest first")
    @GetMapping("/my")
    public ResponseEntity<List<InvoiceView>> mine() {
        return ResponseEntity.ok(service.listMine());
    }

    @Operation(summary = "The invoice of one of my orders (made on the spot if it is missing)")
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<InvoiceView> byOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(service.getByOrder(orderId));
    }

    @Operation(summary = "One invoice as data")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceView> one(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @Operation(summary = "One invoice as a PDF file")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] bytes = service.pdf(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"" + service.fileName(id) + "\"")
                .header("Cache-Control", "no-store")
                .body(bytes);
    }
}
