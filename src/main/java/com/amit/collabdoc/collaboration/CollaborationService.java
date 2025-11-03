package com.amit.collabdoc.collaboration;

import com.amit.collabdoc.document.DocumentService;
import com.amit.collabdoc.dto.DocumentResponse;
import com.amit.collabdoc.exception.ResourceNotFoundException;
import com.amit.collabdoc.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;


import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class CollaborationService {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationService.class);

    // Key prefixes for Redis to keep data organized
    private static final String DOC_CONTENT_KEY_PREFIX = "doc:content:";
    private static final String DIRTY_DOCS_KEY = "doc:dirty";

    private final ValueOperations<String, Object> valueOps;
    private final SetOperations<String, String> setOps;
    private final RedisTemplate<String, String> redisTemplate;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    public CollaborationService(RedisTemplate<String, Object> redisTemplate,
                                DocumentService documentService,
                                RedisTemplate<String, String> stringRedisTemplate,
                                ObjectMapper objectMapper) {
        this.valueOps = redisTemplate.opsForValue();
        this.documentService = documentService;
        this.redisTemplate = stringRedisTemplate;
        this.setOps = stringRedisTemplate.opsForSet();
        this.objectMapper = objectMapper;
    }

    /**
     * Called when a user first joins a document.
     * Fetches the latest content from cache (Redis) or DB (Postgres).
     */
    public Object joinDocument(Long documentId, String username) {
        String docKey = DOC_CONTENT_KEY_PREFIX + documentId;
        // This will be a Map if from Redis, or a JSON String if from DB
        Object content = valueOps.get(docKey);

        if (content == null) {
            logger.info("Cache miss for doc {}. Fetching from DB.", documentId);
            try {
                // --- THIS IS THE FIX ---
                // We now get a DTO from the service, not an Entity
                DocumentResponse docResponse = documentService.getDocumentById(documentId, username);

                // The "content" in Postgres is now a JSON string "{\"ops\":...}"
                // We get it from the DTO
                String contentString = docResponse.getContent();
                // ---------------------

                // We must parse the JSON string from the DB into an Object (Map)
                // so the client *always* receives a JSON object, never a string.
                try {
                    content = objectMapper.readValue(contentString, Object.class);
                } catch (Exception e) {
                    logger.warn("Could not parse DB content for doc {}. Sending empty.", documentId);


                    try {
                        content = objectMapper.readValue("{\"ops\":[{\"insert\":\"\\n\"}]}", Object.class);
                    } catch (JsonProcessingException jsonEx) {
                        // This should *never* happen with our hardcoded string, but we must handle it.
                        logger.error("FATAL: Could not parse default empty delta.", jsonEx);
                        content = new java.util.HashMap<>(); // Send an empty map as a last resort
                    }
                    // -------------------------
                }

                // Store the *Object/Map* in Redis
                valueOps.set(docKey, content, 24, TimeUnit.HOURS);

            } catch (ResourceNotFoundException e) {
                logger.error("User {} tried to join non-existent or unauthorized doc {}", username, documentId);
                throw e;
            }
        } else {
            logger.info("Cache hit for doc {}. Content is type: {}", documentId, content.getClass().getName());
        }

        // This will now *always* return an Object (Map), never a string.
        return content;
    }

    /**
     * Called when a user sends an edit.
     * Applies the change to Redis and marks the document as "dirty".
     */
    public void applyChange(Long documentId, Object fullDelta) {
        String docKey = DOC_CONTENT_KEY_PREFIX + documentId;

        // We now store the *full delta Object* in Redis.
        // Our RedisConfig serializes this as JSON.
        valueOps.set(docKey, fullDelta, 24, TimeUnit.HOURS); // Reset expiration on edit

        // Mark the document as "dirty" so our background task will save it
        markAsDirty(documentId);
    }

    /**
     * Adds the document ID to the "dirty" set in Redis.
     */
    private void markAsDirty(Long documentId) {
        setOps.add(DIRTY_DOCS_KEY, documentId.toString());
        // logger.info("Marked doc {} as dirty.", documentId);
    }
}

