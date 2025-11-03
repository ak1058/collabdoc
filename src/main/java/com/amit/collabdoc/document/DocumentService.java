package com.amit.collabdoc.document;


import com.amit.collabdoc.dto.CreateDocumentRequest;
import com.amit.collabdoc.dto.DocumentResponse;
import com.amit.collabdoc.dto.ShareInfo;
import com.amit.collabdoc.dto.ShareRequest;
import com.amit.collabdoc.exception.AccessDeniedException;
import com.amit.collabdoc.exception.ResourceNotFoundException;
import com.amit.collabdoc.mail.EmailService;
import com.amit.collabdoc.model.Document;
import com.amit.collabdoc.model.DocumentPermission;
import com.amit.collabdoc.model.User;
import com.amit.collabdoc.model.enums.PermissionType;
import com.amit.collabdoc.repository.DocumentPermissionRepository;
import com.amit.collabdoc.repository.DocumentRepository;
import com.amit.collabdoc.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentPermissionRepository documentPermissionRepository;
    private final EmailService emailService;

    // Constructor updated
    public DocumentService ( DocumentRepository documentRepository,
                             UserRepository userRepository,
                             DocumentPermissionRepository documentPermissionRepository,
                             EmailService emailService){
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.documentPermissionRepository = documentPermissionRepository;
        this.emailService = emailService;
    }

    /**
     * Creates a new document for the currently authenticated user.
     * Updated to use CreateDocumentRequest DTO.
     */
    @Transactional
    public Document createDocument (CreateDocumentRequest request, String username){
        User owner = getUserByUsername(username);

        String docTitle = (request.getTitle() == null || request.getTitle().isBlank()) ? "Untitled Document": request.getTitle();

//         A valid empty Quill delta as a JSON string
        String defaultContent = "{\"ops\":[{\"insert\":\"\\n\"}]}";

        Document document = new Document(docTitle, owner, defaultContent);
        return documentRepository.save(document);
    }

    /**
     * Updates the title of a document.
     * Only the owner or an EDITOR can perform this action.
     */
    @Transactional
    public Document updateDocumentTitle (Long documentId, String newTitle, String username){
        User user = getUserByUsername(username);
        Document document = getDocumentById(documentId); // Helper that finds or throws 404

        // Check for permission (this is the new logic)
        if (!hasEditPermission(document, user)) {
            throw new AccessDeniedException("You do not have permission to edit this document.");
        }

        document.setTitle(newTitle);
        return documentRepository.save(document);
    }

    /**
     * Deletes a document.
     * Only the owner can perform this action.
     */
    @Transactional
    public void deleteDocument(Long documentId, String username){
        User user = getUserByUsername(username);
        Document document = getDocumentById(documentId);

        // Check if the user is the owner
        if (!document.getOwner().equals(user)) {
            // Use our custom 403 Forbidden exception
            throw new AccessDeniedException("Only the owner can delete this document.");
        }

        documentRepository.delete(document);
    }

    /**
     * NEW: Retrieves a single document by its ID, *if* the user has permission.
     * This is the method our CollaborationService and Controller will use.
     */
    @Transactional(readOnly = true)
    public Document getDocumentById(Long documentId, String username) {
        User user = getUserByUsername(username);
        Document document = getDocumentById(documentId); // Uses private helper

        // Check if user is owner OR has any permission (VIEWER or EDITOR)
        if (document.getOwner().equals(user)) {
            return document; // Owner always has access
        }

        // Check for a specific permission entry
        boolean hasPermission = documentPermissionRepository.findByDocumentAndUser(document, user).isPresent();

        if (!hasPermission) {
            throw new AccessDeniedException("You do not have permission to view this document.");
        }

        return document;
    }

    /**
     * Gets all documents owned by the user.
     * (We can expand this later to include "shared with me" docs)
     * UPDATED: Now returns List<DocumentResponse> as the controller expects.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments(String username){
        User user = getUserByUsername(username);
        List<Document> documents = documentRepository.findByOwnerOrderByUpdatedAtDesc(user);

        // TODO (v2): Also fetch documents shared *with* this user

        return documents.stream()
                .map(doc -> mapToDocumentResponse(doc, false)) // Use helper
                .collect(Collectors.toList());
    }

    /**
     * Public-facing helper to check for edit permissions.
     * This is what our CollaborationController will call.
     * @param documentId The ID of the document.
     * @param username The username of the user.
     * @return true if the user has edit permissions, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean hasEditPermission(Long documentId, String username) {
        // Find the objects first
        User user = getUserByUsername(username);
        Document document = getDocumentById(documentId);
        // Call the private logic
        return hasEditPermission(document, user);
    }

    /**
     * Shares a document with another user and sends an email notification.
     * @param documentId The ID of the document to share.
     * @param shareRequest The ShareRequest DTO (email, permission).
     * @param ownerUsername The username of the person initiating the share.
     */
    @Transactional
    public DocumentPermission shareDocument (Long documentId, ShareRequest shareRequest, String ownerUsername) {
        User owner = getUserByUsername(ownerUsername);
        Document document = getDocumentById(documentId);

        // 1. Security Check: Only the owner can share.
        if (!document.getOwner().equals(owner)) {
            logger.warn("Non-owner {} tried to share doc {}", ownerUsername, documentId);
            throw new AccessDeniedException("Only the document owner can share this document.");
        }
        // 2. Find the user to invite by their email
        String invitedEmail = shareRequest.getEmail();
        User invitedUser = userRepository.findByEmail(invitedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", invitedEmail));

        // 3. Sanity Check: Owner can't share with themselves
        if (invitedUser.equals(owner)) {
            logger.warn("Owner {} tried to share doc {} with themselves.", ownerUsername, documentId);
            throw new IllegalArgumentException("You cannot share a document with yourself.");
        }

        // 4. Check for existing permission
        Optional<DocumentPermission> existingPermission = documentPermissionRepository.findByDocumentAndUser(document, invitedUser);
        DocumentPermission permission;

        if (existingPermission.isPresent()){
            // 5a. If permission exists, update it
            logger.info("Updating permission for user {} on doc {}", invitedUser.getUsername(), documentId);
            permission = existingPermission.get();
            permission.setPermission(shareRequest.getPermission());
        }else {
            // 5b. If not, create a new permission
            logger.info("Creating new permission for user {} on doc {}", invitedUser.getUsername(), documentId);
            permission = new DocumentPermission(document, invitedUser, shareRequest.getPermission());
        }

        // 6. Save the new/updated permission
        DocumentPermission savedPermission = documentPermissionRepository.save(permission);

        // 7. Send email notification asynchronously
        emailService.sendShareNotificationEmail(
                invitedUser.getEmail(),
                owner.getUsername(),
                document.getTitle(),
                permission.getPermission().name()
        );

        return savedPermission;


    }

    // --- NEW METHOD ---
    /**
     * Gets all documents that have been shared with the authenticated user.
     * @param username The email/username of the user.
     * @return A list of DocumentResponse DTOs.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsSharedWithUser(String username) {
        User user = getUserByUsername(username);

        // 1. Find all permission entries for this user
        List<DocumentPermission> permissions = documentPermissionRepository
                .findByUserOrderByDocumentUpdatedAtDesc(user);

        // 2. Map the Document from each permission to a DocumentResponse
        return permissions.stream()
                .map(permission -> {
                    // Get the document from the permission
                    Document document = permission.getDocument();
                    // Map it to a DTO (false = don't include content)
                    return mapToDocumentResponse(document, false);
                })
                .collect(Collectors.toList());
    }

    // --- Private Helper Methods ---

    /**
     * Helper to check if a user is the owner OR has EDITOR permission.
     */
    private boolean hasEditPermission(Document document, User user) {
        // 1. Check if owner
        if (document.getOwner().equals(user)) {
            return true;
        }
        // 2. Check if they have an EDITOR permission
        return documentPermissionRepository.findByDocumentAndUser(document, user)
                .map(permission -> permission.getPermission() == PermissionType.EDITOR)
                .orElse(false); // If no permission entry, default to false
    }

    /**
     *  Helper to find a user or throw a standard error.
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     *  Helper to find a document or throw a standard error.
     */
    private Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }

    /**
     * Helper to map a Document Entity to a DTO (with optional content).
     * UPDATED to include the list of shared users.
     */
    public DocumentResponse mapToDocumentResponse(Document document, boolean includeContent) {
        // This is safe because we're in a @Transactional method
        // We get the permissions from the document and map them to DTOs
        List<ShareInfo> sharedWith = document.getPermissions().stream()
                .map(permission -> new ShareInfo(
                        permission.getUser().getUsername(),
                        permission.getUser().getEmail(),
                        permission.getPermission()
                ))
                .collect(Collectors.toList());

        DocumentResponse dto = new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getOwner().getUsername(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );

        dto.setSharedWith(sharedWith); // Set the list

        if (includeContent) {
            dto.setContent(document.getContent());
        }
        return dto;
    }
}

