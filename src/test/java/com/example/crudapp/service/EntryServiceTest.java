package com.example.crudapp.service;

import com.example.crudapp.model.Entry;
import com.example.crudapp.repository.EntryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntryServiceTest {
    @Mock
    private EntryRepository entryRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EntryService entryService;

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
    void getAllEntries_ShouldReturnEntriesFromDatabaseWhenCacheMiss() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("all_entries")).thenReturn(null);
        when(entryRepository.findAll()).thenReturn(testEntries);
        when(objectMapper.writeValueAsString(testEntries)).thenReturn("json-data");

        // Act
        List<Entry> result = entryService.getAllEntries();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(entryRepository).findAll();
        verify(valueOperations).set(eq("all_entries"), eq("json-data"), eq(60L), any());
    }

    @Test
    void getAllEntries_ShouldFallbackToDatabaseWhenRedisFails() {
        // Arrange
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));
        when(entryRepository.findAll()).thenReturn(testEntries);

        // Act
        List<Entry> result = entryService.getAllEntries();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(entryRepository).findAll();
    }

    @Test
    void getEntryById_ShouldReturnEntryFromDatabaseWhenCacheMiss() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("entry_1")).thenReturn(null);
        when(entryRepository.findById(1L)).thenReturn(Optional.of(testEntry));
        when(objectMapper.writeValueAsString(testEntry)).thenReturn("json-data");

        // Act
        Entry result = entryService.getEntryById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(entryRepository).findById(1L);
        verify(valueOperations).set(eq("entry_1"), eq("json-data"), eq(60L), any());
    }

    @Test
    void getEntryById_ShouldReturnNullWhenEntryNotFound() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("entry_1")).thenReturn(null);
        when(entryRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Entry result = entryService.getEntryById(1L);

        // Assert
        assertNull(result);
    }

    @Test
    void createEntry_ShouldSaveEntryAndClearCache() {
        // Arrange
        Entry newEntry = new Entry(150.0, "New entry", LocalDate.of(2024, 1, 20));
        when(entryRepository.save(newEntry)).thenReturn(testEntry);

        // Act
        Entry result = entryService.createEntry(newEntry);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(entryRepository).save(newEntry);
        verify(redisTemplate).delete("all_entries");
    }

    @Test
    void deleteEntry_ShouldDeleteEntryAndClearCache() {
        // Arrange
        when(entryRepository.findById(1L)).thenReturn(Optional.of(testEntry));

        // Act
        boolean result = entryService.deleteEntry(1L);

        // Assert
        assertTrue(result);
        verify(entryRepository).deleteById(1L);
        verify(redisTemplate).delete("all_entries");
        verify(redisTemplate).delete("entry_1");
    }

    @Test
    void deleteEntry_ShouldReturnFalseWhenEntryNotFound() {
        // Arrange
        when(entryRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        boolean result = entryService.deleteEntry(1L);

        // Assert
        assertFalse(result);
        verify(entryRepository, never()).deleteById(1L);
    }

    @Test
    void updateEntry_ShouldUpdateExistingEntry() {
        // Arrange
        Entry updatedDetails = new Entry(200.0, "Updated description", LocalDate.of(2024, 1, 16));
        when(entryRepository.findById(1L)).thenReturn(Optional.of(testEntry));
        when(entryRepository.save(testEntry)).thenReturn(testEntry);

        // Act
        Entry result = entryService.updateEntry(1L, updatedDetails);

        // Assert
        assertNotNull(result);
        assertEquals(200.0, result.getAmount());
        assertEquals("Updated description", result.getDescription());
        verify(entryRepository).save(testEntry);
        verify(redisTemplate).delete("all_entries");
        verify(redisTemplate).delete("entry_1");
    }

    @Test
    void updateEntry_ShouldReturnNullWhenEntryNotFound() {
        // Arrange
        Entry updatedDetails = new Entry(200.0, "Updated", LocalDate.of(2024, 1, 16));
        when(entryRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Entry result = entryService.updateEntry(1L, updatedDetails);

        // Assert
        assertNull(result);
        verify(entryRepository, never()).save(any());
    }
    @Test
    void clearAllCaches_ShouldClearAllCaches() {
        // Act
        entryService.clearAllCaches();

        // Assert
        verify(redisTemplate).delete("all_entries");
    }
    @Test
    void deleteAllEntries_ShouldDeleteAllEntriesAndClearCache() {
        // Arrange
        doNothing().when(entryRepository).deleteAll();

        // Act
        entryService.deleteAllEntries();

        // Assert
        verify(entryRepository).deleteAll();
        verify(redisTemplate).delete("all_entries");
        // Note: In a real implementation, you might want to clear all individual entry caches too
    }

    @Test
    void deleteAllEntries_ShouldHandleException() {
        // Arrange
        doThrow(new RuntimeException("DB error")).when(entryRepository).deleteAll();

        // Act & Assert - Should throw exception since we re-throw it
        Exception exception = assertThrows(RuntimeException.class, () -> {
            entryService.deleteAllEntries();
        });
        
        assertEquals("DB error", exception.getMessage());
        
        // Verify cache clearing was still attempted despite the exception
        verify(redisTemplate).delete("all_entries");
    }
}