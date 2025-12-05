package com.example.crudapp.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EntryTest {

    @Test
    void shouldCreateEntryWithConstructor() {
        // Arrange & Act
        Entry entry = new Entry(100.0, "Test description", LocalDate.of(2024, 1, 15));
        
        // Assert
        assertNull(entry.getId());
        assertEquals(100.0, entry.getAmount());
        assertEquals("Test description", entry.getDescription());
        
        assertEquals(LocalDate.of(2024, 1, 15), entry.getDate());
    }

    @Test
    void shouldSetAndGetProperties() {
        // Arrange
        Entry entry = new Entry();
        
        // Act
        entry.setId(1L);
        entry.setAmount(200.0);
        entry.setDescription("Updated description");

        entry.setDate(LocalDate.of(2024, 1, 20));
        
        // Assert
        assertEquals(1L, entry.getId());
        assertEquals(200.0, entry.getAmount());
        assertEquals("Updated description", entry.getDescription());
        assertEquals(LocalDate.of(2024, 1, 20), entry.getDate());
    }

    @Test
    void toString_ShouldReturnNonEmptyString() {
        // Arrange
        Entry entry = new Entry(100.0, "Test", LocalDate.of(2024, 1, 15));
        entry.setId(1L);
        
        // Act
        String result = entry.toString();
        
        // Assert - Just verify it returns a non-empty string
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
    
    @Test
    void shouldCreateEntryWithBasicConstructor() {
        // Arrange & Act - For Release 1.0 (without date)
        Entry entry = new Entry(100.0, "Test description");
        
        // Assert
        assertNull(entry.getId());
        assertEquals(100.0, entry.getAmount());
        assertEquals("Test description", entry.getDescription());
    }
}