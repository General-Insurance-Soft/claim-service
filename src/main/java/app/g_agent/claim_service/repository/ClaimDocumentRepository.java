package app.g_agent.claim_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.g_agent.claim_service.model.ClaimDocument;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {
    Optional<ClaimDocument> findById(Long id);

    Optional<List<ClaimDocument>> findByClaimId(Long claimId);
}