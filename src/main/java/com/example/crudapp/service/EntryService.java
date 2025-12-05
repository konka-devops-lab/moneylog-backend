package com.example.crudapp.service;

import com.example.crudapp.model.Entry;
import com.example.crudapp.repository.EntryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class EntryService {
    
    private static final Logger logger = LoggerFactory.getLogger(EntryService.class);
    private static final String ALL_ENTRIES_CACHE_KEY = "all_entries";
    private static final String ENTRY_CACHE_KEY_PREFIX = "entry_";
    private static final int CACHE_TTL = 60; // seconds
    
    @Autowired
    private EntryRepository entryRepository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public List<Entry> getAllEntries() {
        try {
            // Try to get from cache first
            String cachedData = redisTemplate.opsForValue().get(ALL_ENTRIES_CACHE_KEY);
            
            if (cachedData != null) {
                logger.info("Serving all entries from Redis cache");
                return objectMapper.readValue(cachedData, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Entry.class));
            } else {
                logger.info("Cache miss: No cache found for all entries, fetching from database");
            }
            
            // Fetch from database
            List<Entry> entries = entryRepository.findAll();
            
            // Cache the result
            logger.info("Serving all entries from Database and caching the result");
            String jsonData = objectMapper.writeValueAsString(entries);
            redisTemplate.opsForValue().set(ALL_ENTRIES_CACHE_KEY, jsonData, CACHE_TTL, TimeUnit.SECONDS);
            
            return entries;
            
        } catch (JsonProcessingException e) {
            logger.error("Error processing JSON for cache", e);
            // Fallback to database only
            return entryRepository.findAll();
        } catch (Exception e) {
            logger.error("Redis Fetch Error", e);
            // Fallback to database only
            return entryRepository.findAll();
        }
    }
    
    public Entry getEntryById(Long id) {
        String cacheKey = ENTRY_CACHE_KEY_PREFIX + id;
        
        try {
            // Try to get from cache first
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedData != null) {
                logger.info("Serving entry {} from Redis cache", id);
                return objectMapper.readValue(cachedData, Entry.class);
            } else {
                logger.info("Cache miss: No cache found for entry {}, fetching from database", id);
            }
            
            // Fetch from database
            Optional<Entry> entry = entryRepository.findById(id);
            
            if (entry.isPresent()) {
                // Cache the result
                logger.info("Serving entry {} from Database and caching the result", id);
                String jsonData = objectMapper.writeValueAsString(entry.get());
                redisTemplate.opsForValue().set(cacheKey, jsonData, CACHE_TTL, TimeUnit.SECONDS);
                
                return entry.get();
            }
            
            return null;
            
        } catch (JsonProcessingException e) {
            logger.error("Error processing JSON for cache", e);
            // Fallback to database only
            return entryRepository.findById(id).orElse(null);
        } catch (Exception e) {
            logger.error("Redis Fetch Error for entry {}", id, e);
            // Fallback to database only
            return entryRepository.findById(id).orElse(null);
        }
    }
    
    public Entry createEntry(Entry entry) {
        Entry savedEntry = entryRepository.save(entry);
        logger.info("Inserted entry with ID: {}", savedEntry.getId());
        
        // Clear the cache because data changed
        clearAllEntriesCache();
        
        return savedEntry;
    }
    
    public boolean deleteEntry(Long id) {
        Optional<Entry> entry = entryRepository.findById(id);
        
        if (entry.isPresent()) {
            entryRepository.deleteById(id);
            logger.info("Deleted entry with ID: {}", id);
            
            // Clear relevant caches because data changed
            clearAllEntriesCache();
            clearEntryCache(id);
            
            return true;
        }
        
        logger.warn("Delete failed: Entry with ID {} not found", id);
        return false;
    }

    public void deleteAllEntries() {
        try {
            entryRepository.deleteAll();
            logger.info("Deleted all entries");
            
            // Clear all caches
            clearAllEntriesCache();
            // Note: In production, you might want to clear all entry_* keys using Redis patterns
            
        } catch (Exception e) {
            logger.error("Error deleting all entries", e);
            // Still try to clear cache even if DB operation fails
            clearAllEntriesCache();
            throw e; // Re-throw to let controller handle it
        }
    }

    
    // ========== RELEASE 3.0 - START (Update Functionality) ==========
    public Entry updateEntry(Long id, Entry entryDetails) {
        Optional<Entry> optionalEntry = entryRepository.findById(id);
        
        if (optionalEntry.isPresent()) {
            Entry existingEntry = optionalEntry.get();
            existingEntry.setAmount(entryDetails.getAmount());
            existingEntry.setDescription(entryDetails.getDescription());

            
            existingEntry.setDate(entryDetails.getDate());
            
            Entry updatedEntry = entryRepository.save(existingEntry);
            logger.info("Updated entry with ID: {}", id);
            
            // Clear relevant caches because data changed
            clearAllEntriesCache();
            clearEntryCache(id);
            
            return updatedEntry;
        }
        
        logger.warn("Update failed: Entry with ID {} not found", id);
        return null;
    }
    // ========== RELEASE 3.0 - END ==========
    
    private void clearAllEntriesCache() {
        try {
            redisTemplate.delete(ALL_ENTRIES_CACHE_KEY);
            logger.info("Cache cleared for {}", ALL_ENTRIES_CACHE_KEY);
        } catch (Exception e) {
            logger.error("Error clearing all entries cache", e);
        }
    }
    
    private void clearEntryCache(Long id) {
        try {
            String cacheKey = ENTRY_CACHE_KEY_PREFIX + id;
            redisTemplate.delete(cacheKey);
            logger.info("Cache cleared for {}", cacheKey);
        } catch (Exception e) {
            logger.error("Error clearing entry cache for ID: {}", id, e);
        }
    }
    
    public void clearAllCaches() {
        try {
            // Clear all entries cache
            redisTemplate.delete(ALL_ENTRIES_CACHE_KEY);
            
            // Clear all individual entry caches (this is a simplified approach)
            // In production, you might want to use Redis patterns to delete all entry_* keys
            logger.info("All caches cleared");
        } catch (Exception e) {
            logger.error("Error clearing all caches", e);
        }
    }
}