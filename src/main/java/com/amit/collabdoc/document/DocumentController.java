package com.amit.collabdoc.document;

import com.amit.collabdoc.dto.CreateDocumentRequest;
import com.amit.collabdoc.dto.DocumentResponse;
import com.amit.collabdoc.dto.ShareRequest;
import com.amit.collabdoc.dto.UpdateDocumentRequest;
import com.amit.collabdoc.model.Document;
import com.amit.collabdoc.model.DocumentPermission;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * POST /api/documents : Creates a new document.
     * The authenticated user will be the owner.
     */
    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument (@RequestBody CreateDocumentRequest request, Principal principal){
        String username = principal.getName();
        Document document = documentService.createDocument(request, username);
        return new ResponseEntity<>(mapToDocumentResponse(document), HttpStatus.CREATED);
    }

    /**
     * GET /api/documents : Gets all documents for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments (Principal principal){
        String username = principal.getName();
        // Service now returns the DTO list directly
        List<DocumentResponse> documents = documentService.getAllDocuments(username);
        return ResponseEntity.ok(documents);
    }

    /**
     * GET /api/documents/{id} : Gets a single document by its ID.
     * This will only succeed if the user is the owner or has been granted permission.
     * UPDATED: Returns a DocumentResponse DTO, including the content.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById (@PathVariable Long id, Principal principal){

        Document document = documentService.getDocumentById(id, principal.getName());

        // Map to DTO, *including* content
        return ResponseEntity.ok(mapToDocumentResponseWithContent(document));
    }

    /**
     * DELETE /api/documents/{id} : Deletes a document.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, Principal principal) {
        String username = principal.getName();
        documentService.deleteDocument(id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/documents/{id} : Updates a document's title.
     * This is our "Slow Data" path.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> updateDocumentTitle (@RequestBody UpdateDocumentRequest updateRequest, @PathVariable Long id, Principal principal) { // Use Principal
        String username = principal.getName();
        Document updatedDocument = documentService.updateDocumentTitle(id, updateRequest.getTitle(), username);
        return ResponseEntity.ok(mapToDocumentResponse(updatedDocument));
    }

    /**
     * POST /api/documents/{id}/share : Shares a document with another user.
     * @param id The ID of the document to share.
     * @param shareRequest The ShareRequest DTO (email, permission).
     * @param principal The authenticated user (must be the owner).
     * @return The new permission object.
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareDocument (@PathVariable Long id,
                                            @RequestBody ShareRequest shareRequest,
                                            Principal principal) {
        String ownerUsername = principal.getName();
        // We can create a DTO for this response, but for now we'll return a simple map
        DocumentPermission permission = documentService.shareDocument(id, shareRequest, ownerUsername);

        return ResponseEntity.ok(Map.of(
                "message", "Document shared successfully with " + shareRequest.getEmail(),
                "permissionId", permission.getId(),
                "permissionLevel", permission.getPermission()
        ));
    }

    /**
     * GET /api/documents/shared-with-me : Gets all documents shared with the authenticated user.
     */
    @GetMapping("/shared-with-me")
    public ResponseEntity<List<DocumentResponse>> getSharedDocuments(Principal principal) {
        String username = principal.getName();
        List<DocumentResponse> documents = documentService.getDocumentsSharedWithUser(username);
        return ResponseEntity.ok(documents);
    }

    /**
     * Helper to map a Document Entity to a DTO (for lists, no content).
     */
    private DocumentResponse mapToDocumentResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getOwner().getUsername(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    /**
     *  Maps a Document Entity to a DTO, *including* the content.
     */
    private DocumentResponse mapToDocumentResponseWithContent(Document document) {
        DocumentResponse response = new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getOwner().getUsername(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
        // Explicitly set the content
        response.setContent(document.getContent());
        return response;
    }
}

