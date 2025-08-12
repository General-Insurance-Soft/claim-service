package app.g_agent.claim_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.g_agent.claim_service.model.Claim;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findById(Long id);

    Optional<Claim> findByIdAndCompanyId(Long id, Long companyId);

    Optional<List<Claim>> findByCompanyId(Long companyId);

    @Query("SELECT DISTINCT c FROM Claim c LEFT JOIN FETCH c.claimDocuments WHERE c.id IN :ids")
    List<Claim> findAllWithDocumentsByIds(@Param("ids") Set<Long> ids);

    List<Claim> findByContactIdAndCompanyId(Long contactId, Long companyId);

    @Query(value = """
                        SELECT * FROM (
                            SELECT
                            p.claim_date,
                            p.company_id,
                            p.contact_id,
                            p.created_at,
                            p.id,
                            p.payment_method,
                            p.updated_at,
                            p.updated_by,
                            p.claim_number,
                            p.policy_number,
                            p.status,
                            sub.claim_count
                            FROM (
                                SELECT *,
                                       ROW_NUMBER() OVER (PARTITION BY contact_id ORDER BY created_at DESC) AS row_num
                                FROM claim
                                WHERE company_id = :companyId
                            ) p
                            JOIN (
                                SELECT contact_id, COUNT(*) AS claim_count
                                FROM claim
                                WHERE company_id = :companyId
                                GROUP BY contact_id
                            ) sub ON p.contact_id = sub.contact_id
                            WHERE p.row_num = 1
                        ) result
                        """, countQuery = """
            SELECT COUNT(*) FROM (
                SELECT contact_id
                FROM claim
                WHERE company_id = :companyId
                GROUP BY contact_id
            ) AS count_table
            """, nativeQuery = true)
    Page<Object[]> findLatestClaimsPerContact(Pageable pageable, @Param("companyId") Long companyId);
}