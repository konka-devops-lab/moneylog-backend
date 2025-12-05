package com.example.crudapp.controller;

import com.example.crudapp.model.Entry;
import com.example.crudapp.service.EntryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EntryController.class)
class EntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntryService entryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Entry testEntry;
    private List<Entry> testEntries;

    @BeforeEach
    void setUp() {
        testEntry = new Entry(100.0, "Test groceries", LocalDate.of(2024, 1, 15));
        testEntry.setId(1L);
        
        testEntries = Arrays.asList(
            new Entry(100.0, "Groceries", LocalDate.of(2024, 1, 15)),
            new Entry(200.0, "Rent", LocalDate.of(2024, 1, 1))
        );
    }

    @Test
    void getAllEntries_ShouldReturnEntries() throws Exception {
        // Arrange
        when(entryService.getAllEntries()).thenReturn(testEntries);

        // Act & Assert
        mockMvc.perform(get("/api/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Groceries"));
    }

    @Test
    void getEntryById_ShouldReturnEntry() throws Exception {
        // Arrange
        when(entryService.getEntryById(1L)).thenReturn(testEntry);

        // Act & Assert
        mockMvc.perform(get("/api/entries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.description").value("Test groceries"))
                .andExpect(jsonPath("$.date").value("2024-01-15"));
    }

    @Test
    void getEntryById_ShouldReturn404WhenNotFound() throws Exception {
        // Arrange
        when(entryService.getEntryById(1L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/entries/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Entry not found"));
    }
    @Test
    void createEntry_ShouldCreateNewEntry() throws Exception {
        // Arrange
        Entry newEntry = new Entry(150.0, "New entry", LocalDate.of(2024, 1, 20));
        when(entryService.createEntry(any(Entry.class))).thenReturn(testEntry);

        // Act & Assert
        mockMvc.perform(post("/api/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newEntry)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createEntry_ShouldReturn400WhenMissingAmount() throws Exception {
        // Arrange - Use raw JSON that bypasses @Valid but triggers your custom validation
        String invalidJson = "{\"description\": \"Valid description\", \"date\": \"2024-01-15\"}";

        // Act & Assert - Just check for 400 status, don't check error message format
        mockMvc.perform(post("/api/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEntry_ShouldReturn400WhenMissingDescription() throws Exception {
        // Arrange
        String invalidJson = "{\"amount\": 100.0, \"date\": \"2024-01-15\"}";

        // Act & Assert
        mockMvc.perform(post("/api/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
    @Test
    void createEntry_ShouldReturn400WhenMissingDate() throws Exception {
        // Arrange
        String invalidJson = "{\"amount\": 100.0, \"description\": \"Valid description\"}";

        // Act & Assert
        mockMvc.perform(post("/api/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEntry_ShouldReturn400WhenEmptyDescription() throws Exception {
        // Arrange - This should trigger your custom validation for empty description
        String invalidJson = "{\"amount\": 100.0, \"description\": \"\", \"date\": \"2024-01-15\"}";

        // Act & Assert
        mockMvc.perform(post("/api/entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEntry_ShouldUpdateEntry() throws Exception {
        // Arrange
        Entry updatedEntry = new Entry(200.0, "Updated description", LocalDate.of(2024, 1, 16));
        testEntry.setAmount(200.0);
        testEntry.setDescription("Updated description");
        
        when(entryService.updateEntry(eq(1L), any(Entry.class))).thenReturn(testEntry);

        // Act & Assert
        mockMvc.perform(put("/api/entries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedEntry)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200.0))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void updateEntry_ShouldReturn400WhenMissingFields() throws Exception {
        // Arrange
        String invalidJson = "{\"description\": \"\", \"date\": \"2024-01-15\"}";

        // Act & Assert
        mockMvc.perform(put("/api/entries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEntry_ShouldReturn404WhenEntryNotFound() throws Exception {
        // Arrange
        Entry updatedEntry = new Entry(200.0, "Updated", LocalDate.of(2024, 1, 16));
        when(entryService.updateEntry(eq(1L), any(Entry.class))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(put("/api/entries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedEntry)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Entry not found"));
    }
    @Test
    void deleteEntry_ShouldDeleteEntry() throws Exception {
        // Arrange
        when(entryService.deleteEntry(1L)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/entries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Entry deleted successfully"));
    }

    @Test
    void deleteEntry_ShouldReturn404WhenEntryNotFound() throws Exception {
        // Arrange
        when(entryService.deleteEntry(1L)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/entries/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Entry not found"));
    }

    @Test
    void deleteAllEntries_ShouldDeleteAllEntries() throws Exception {
        // Arrange
        doNothing().when(entryService).deleteAllEntries();

        // Act & Assert
        mockMvc.perform(delete("/api/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All entries deleted successfully"));
    }

    @Test
    void deleteAllEntries_ShouldReturn500WhenServiceFails() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Database error")).when(entryService).deleteAllEntries();

        // Act & Assert
        mockMvc.perform(delete("/api/entries"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to delete all entries"));
    }
}