package com.example.crudapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entries")
public class Entry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Amount is required")
    @Column(nullable = false)
    private Double amount;
    
    @NotBlank(message = "Description is required")
    @Column(nullable = false)
    private String description;
    
    // ========== RELEASE 2.0 - START (Add Date Field) ==========
    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;
    // ========== RELEASE 2.0 - END ==========
    
    // Default constructor
    public Entry() {}
    
    public Entry(Double amount, String description) {
        this.amount = amount;
        this.description = description;
    }
    
    // ========== RELEASE 2.0 - START (Enhanced Constructor with Date) ==========
    public Entry(Double amount, String description, LocalDate date) {
        this.amount = amount;
        this.description = description;
        this.date = date;
    }
    // ========== RELEASE 2.0 - END ==========
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Double getAmount() {
        return amount;
    }
    
    public void setAmount(Double amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    // ========== RELEASE 2.0 - START (Date Getters/Setters) ==========
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    // ========== RELEASE 2.0 - END ==========
    
    @Override
    public String toString() {
        return "Entry{" +
                "id=" + id +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                // ========== RELEASE 2.0 - START (Include Date in toString) ==========
                ", date=" + date +
                // ========== RELEASE 2.0 - END ==========
                '}';
    }
}