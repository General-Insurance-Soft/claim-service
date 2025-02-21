package app.g_agent.claim_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.g_agent.claim_service.model.Claim;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findById(Long id);

    Optional<Claim> findByIdAndCompanyId(Long id, Long companyId);

    Optional<List<Claim>> findByCompanyId(Long companyId);
}