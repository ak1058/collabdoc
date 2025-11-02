package com.amit.collabdoc.collaboration;

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

    public CollaborationController(CollaborationService collaborationService, SimpMessagingTemplate messagingTemplate) {
        this.collaborationService = collaborationService;
        this.messagingTemplate = messagingTemplate;
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
     * UPDATED: No longer uses @SendTo. Broadcasts manually to add sender info.
     *
     * @param docId  The ID of the document being edited.
     * @param change The EditorChange DTO (containing delta and fullDelta)
     * @param principal The authenticated user.
     */
    @MessageMapping("/document/{docId}/edit")
    // @SendTo("/topic/document/{docId}/changes")
    public void handleEdit(@DestinationVariable Long docId, @Payload EditorChange change, Principal principal) {

        if (principal == null) {
            logger.warn("Unauthorized edit attempt on doc {}", docId);
            return;
        }

        String username = principal.getName();
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

