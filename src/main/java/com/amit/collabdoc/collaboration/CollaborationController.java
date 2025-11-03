package com.amit.collabdoc.collaboration;

import com.amit.collabdoc.document.DocumentService; // NEW IMPORT
import com.amit.collabdoc.dto.EditorChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class CollaborationController {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationController.class);

    private final CollaborationService collaborationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentService documentService; // NEW: For permission checks

    public CollaborationController(CollaborationService collaborationService,
                                   SimpMessagingTemplate messagingTemplate,
                                   DocumentService documentService) { // UPDATED CONSTRUCTOR
        this.collaborationService = collaborationService;
        this.messagingTemplate = messagingTemplate;
        this.documentService = documentService; // NEW
    }

    @MessageMapping("/document/{docId}/join")
    public void joinDocument(@DestinationVariable Long docId, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String username = principal.getName();
        logger.info("User {} joining document {}", username, docId);

        try {
            // 1. Fetch the document content (now always a JSON Object)
            Object currentContent = collaborationService.joinDocument(docId, username);

            String sessionId = headerAccessor.getSessionId();

            // 2. Send the full document content *only* to the user who just joined.
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/topic/document/" + docId + "/content",
                    currentContent,
                    headerAccessor.getMessageHeaders()
            );

        } catch (Exception e) {
            logger.error("Error on join for user {}: {}", username, e.getMessage());
        }
    }

    /**
     * Handles incoming edits from a user.
     * UPDATED: Now checks for EDITOR permission before applying changes.
     *
     * @param docId  The ID of the document being edited.
     * @param change The EditorChange DTO (containing delta and fullDelta)
     * @param principal The authenticated user.
     */
    @MessageMapping("/document/{docId}/edit")
    // @SendTo("/topic/document/{docId}/changes")
    public void handleEdit(@DestinationVariable Long docId, @Payload EditorChange change, Principal principal) {

        if (principal == null) {
            logger.warn("Unauthorized edit attempt on doc {}: No principal.", docId);
            return;
        }

        String username = principal.getName();

        // --- THIS IS THE SECURITY FIX ---
        // Before we do anything, check if the user has permission to edit.
        try {
            if (!documentService.hasEditPermission(docId, username)) {
                logger.warn("User {} (VIEWER) tried to edit doc {}. Access denied.", username, docId);
                // We just stop. No save, no broadcast.
                return;
            }
        } catch (Exception e) {
            // This could be a ResourceNotFoundException or AccessDeniedException
            logger.error("Error checking edit permission for user {} on doc {}: {}", username, docId, e.getMessage());
            return;
        }
        // --------------------------------

        // logger.info("User {} sent change to document {}", username, docId);

        // 1. Save the FULL DELTA OBJECT to Redis and mark as "dirty"
        collaborationService.applyChange(docId, change.getFullDelta());


        // 2. Create a new payload that includes the sender's username
        Map<String, Object> payload = Map.of(
                "delta", change.getDelta(),
                "sender", username
        );

        // 3. Manually broadcast the payload to the public topic
        messagingTemplate.convertAndSend("/topic/document/" + docId + "/changes", payload);
    }
}

