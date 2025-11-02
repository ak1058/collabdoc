package com.amit.collabdoc.persistence;

import com.amit.collabdoc.model.Document;
import com.amit.collabdoc.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;


import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This service runs in the background to persist "dirty" documents
 * from the Redis cache to the Postgres database.
 */
@Service
public class DocumentPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentPersistenceService.class);

    private static final String DOC_CONTENT_KEY_PREFIX = "doc:content:";
    private static final String DIRTY_DOCS_KEY = "doc:dirty";

    private final SetOperations<String, String> setOps;
    private final ValueOperations<String, Object> valueOps;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    public DocumentPersistenceService(RedisTemplate<String, String> stringRedisTemplate,
                                      RedisTemplate<String, Object> redisTemplate,
                                      DocumentRepository documentRepository,
                                      ObjectMapper objectMapper) { // NEW
        this.setOps = stringRedisTemplate.opsForSet();
        this.valueOps = redisTemplate.opsForValue();
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper; // NEW
    }

    @Async
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void persistDirtyDocuments() {
        Set<String> dirtyDocIds = setOps.members(DIRTY_DOCS_KEY);

        if (dirtyDocIds == null || dirtyDocIds.isEmpty()) {
            return;
        }

        logger.info("Auto-save task found {} dirty documents. Persisting to DB...", dirtyDocIds.size());

        for (String docIdStr : dirtyDocIds) {
            String docKey = DOC_CONTENT_KEY_PREFIX + docIdStr;
            try {
                Long docId = Long.parseLong(docIdStr);

                // 2. Get the latest content (the fullDelta Object/Map) from Redis
                Object contentObject = valueOps.get(docKey);

                if (contentObject != null) {
                    // 3. Find the document in Postgres
                    documentRepository.findById(docId).ifPresent(doc -> {
                        try {

                            // 4. Serialize the Object/Map into a JSON STRING
                            String contentString = objectMapper.writeValueAsString(contentObject);

                            // 5. Update its content and save
                            doc.setContent(contentString);
                            documentRepository.save(doc);

                            // 6. Remove from "dirty" set *after* successful save.
                            setOps.remove(DIRTY_DOCS_KEY, docIdStr);
                            logger.info("Successfully persisted doc {}", docId);

                        } catch (Exception e) {
                            logger.error("Failed to serialize content for doc {}: {}", docId, e.getMessage());
                            // Don't remove from dirty set, retry next time
                        }
                    });
                } else {
                    // Content expired from Redis? Remove from dirty set.
                    logger.warn("Content for dirty doc {} not found in Redis. Removing from dirty set.", docId);
                    setOps.remove(DIRTY_DOCS_KEY, docIdStr);
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid doc ID in dirty set: {}", docIdStr);
                setOps.remove(DIRTY_DOCS_KEY, docIdStr);
            } catch (Exception e) {
                logger.error("Error persisting doc {}: {}", docIdStr, e.getMessage());
                // We don't remove it from the set, so we can retry on the next run.
            }
        }
    }
}

