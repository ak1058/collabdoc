package com.amit.collabdoc.repository;

import com.amit.collabdoc.model.Document;
import com.amit.collabdoc.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwnerOrderByUpdatedAtDesc(User owner);
}
