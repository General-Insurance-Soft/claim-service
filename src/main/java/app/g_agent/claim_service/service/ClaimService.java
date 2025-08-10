package app.g_agent.claim_service.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.g_agent.claim_service.dto.ClaimDocumentDto;
import app.g_agent.claim_service.dto.ClaimDto;
import app.g_agent.claim_service.dto.ClaimMetadataDto;
import app.g_agent.claim_service.model.Claim;
import app.g_agent.claim_service.model.ClaimDocument;
import app.g_agent.claim_service.model.ClaimMetadata;
import app.g_agent.claim_service.repository.ClaimDocumentRepository;
import app.g_agent.claim_service.repository.ClaimRepository;
import app.g_agent.claim_service.system.DuplicateClaimException;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ClaimService {

	private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

	private ClaimRepository claimRepository;
	private ClaimDocumentRepository claimDocumentRepository;
	private JwtService jwtService;

	public ClaimService(ClaimRepository claimRepository, ClaimDocumentRepository claimDocumentRepository,
			JwtService jwtService) {
		this.claimRepository = claimRepository;
		this.claimDocumentRepository = claimDocumentRepository;
		this.jwtService = jwtService;
	}

	@Transactional
	public void deleteClaim(HttpServletRequest request, Long id) throws Exception {
		Optional<Claim> claimOpt = claimRepository.findById(id);

		if (claimOpt.isPresent()) {
			claimRepository.delete(claimOpt.get());
		} else {
			throw new Exception("The claim cannot be found");
		}
	}

	public ClaimDto getClaimById(HttpServletRequest request, Long id) throws Exception {
		Optional<Claim> claimOpt = claimRepository.findById(id);

		if (claimOpt.isPresent()) {
			Claim claim = claimOpt.get();
			ClaimDto claimDto = new ClaimDto();
			claimDto.setId(claim.getId());
			claimDto.setPolicyNumber(claim.getPolicyNumber());
			claimDto.setClaimNumber(claim.getClaimNumber());
			claimDto.setClaimDate(claim.getClaimDate());
			claimDto.setPaymentMethod(claim.getPaymentMethod());
			claimDto.setCompanyId(claim.getCompanyId());
			claimDto.setContactId(claim.getContactId());
			claimDto.setUpdatedBy(claim.getUpdatedBy());
			claimDto.setCreatedAt(claim.getCreatedAt());
			claimDto.setUpdatedAt(claim.getUpdatedAt());

			Set<ClaimDocumentDto> claimDocumentDtos = claim.getClaimDocuments().stream().map(document -> {
				ClaimDocumentDto documentDto = new ClaimDocumentDto();
				documentDto.setId(document.getId());
				documentDto.setFolderName(document.getFolderName());
				documentDto.setDocumentName(document.getDocumentName());
				documentDto.setBlobUrl(document.getBlobUrl());
				documentDto.setUpdatedBy(document.getUpdatedBy());
				documentDto.setCreatedAt(document.getCreatedAt());
				return documentDto;
			}).collect(Collectors.toSet());

			claimDto.setClaimDocuments(claimDocumentDtos);

			if (claim.getClaimMetadata() != null) {
				ClaimMetadataDto claimMetadataDto = new ClaimMetadataDto();
				claimMetadataDto.setId(claim.getClaimMetadata().getId());
				claimMetadataDto.setClaimId(claim.getId());
				claimMetadataDto.setMetadata(claim.getClaimMetadata().getMetadata());
				claimMetadataDto.setCreatedAt(claim.getClaimMetadata().getCreatedAt());
				claimMetadataDto.setUpdatedAt(claim.getClaimMetadata().getUpdatedAt());
				claimDto.setClaimMetadata(claimMetadataDto);
			}

			return claimDto;
		} else {
			throw new Exception("The claim does not exist");
		}
	}

	@Transactional
	public void createClaim(HttpServletRequest request, ClaimDto claimDto) throws Exception {
		Claim claim = new Claim();

		Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());
		Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
		logger.info("user ID: ==============================>" + userId);

		claim.setPolicyNumber(claimDto.getPolicyNumber());
		claim.setClaimNumber(claimDto.getClaimNumber());
		claim.setClaimDate(claimDto.getClaimDate());
		claim.setPaymentMethod(claimDto.getPaymentMethod());
		claim.setCompanyId(orgId);
		claim.setContactId(claimDto.getContactId());
		claim.setUpdatedBy(Long.valueOf(userId));

		if (claimDto.getClaimDocuments() != null) {
			Set<ClaimDocument> claimDocuments = new HashSet<>();
			claimDto.getClaimDocuments().forEach(documentDto -> {
				ClaimDocument document = new ClaimDocument();
				document.setFolderName(documentDto.getFolderName());
				document.setDocumentName(documentDto.getDocumentName());
				document.setBlobUrl(documentDto.getBlobUrl());
				document.setUpdatedBy(Long.valueOf(userId));
				document.setClaim(claim); // Set the claim reference
				claimDocuments.add(document);
			});
			claim.setClaimDocuments(claimDocuments);
		}

		if (claimDto.getClaimMetadata() != null) {
			ClaimMetadata claimMetadata = new ClaimMetadata();
			claimMetadata.setClaim(claim);
			claimMetadata.setMetadata(claimDto.getClaimMetadata().getMetadata());
			claim.setClaimMetadata(claimMetadata);
		}

		try {
			claimRepository.save(claim);
			claimDocumentRepository.saveAll(claim.getClaimDocuments()); // Save the claim documents
			claimRepository.flush();
			claimDocumentRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
				logger.info("Claim error ==========> id: " + ex.getMessage());

				throw new DuplicateClaimException("This claim already exists.");
			}
			throw ex; // Rethrow if not related to constraint violation
		}
	}

	@Transactional
	public void updateClaim(HttpServletRequest request, ClaimDto claimDto, Long id) throws Exception {
		Optional<Claim> claimOpt = claimRepository.findById(id);

		if (claimOpt.isEmpty()) {
			throw new Exception("The claim cannot be found");
		}

		Claim claim = claimOpt.get();

		Long userId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "user-id").toString());

		claim.setClaimNumber(claimDto.getClaimNumber());
		claim.setPolicyNumber(claimDto.getPolicyNumber());
		claim.setClaimDate(claimDto.getClaimDate());
		claim.setPaymentMethod(claimDto.getPaymentMethod());
		claim.setCompanyId(claimDto.getCompanyId());
		claim.setContactId(claimDto.getContactId());
		claim.setUpdatedBy(Long.valueOf(userId));

		if (claimDto.getClaimDocuments() != null) {
			// Clear the existing collection
			claim.getClaimDocuments().clear();

			// Add the new documents to the collection
			claimDto.getClaimDocuments().forEach(documentDto -> {
				ClaimDocument document = new ClaimDocument();
				document.setFolderName(documentDto.getFolderName());
				document.setDocumentName(documentDto.getDocumentName());
				document.setBlobUrl(documentDto.getBlobUrl());
				document.setUpdatedBy(Long.valueOf(userId));
				document.setClaim(claim); // Set the claim reference
				claim.getClaimDocuments().add(document);
			});
		}

		if (claimDto.getClaimMetadata() != null) {
			ClaimMetadata claimMetadata = claim.getClaimMetadata();
			claimMetadata.setClaim(claim);
			claimMetadata.setMetadata(claimDto.getClaimMetadata().getMetadata());
			claim.setClaimMetadata(claimMetadata);
		}

		try {
			claimRepository.save(claim);
		} catch (DataIntegrityViolationException ex) {
			if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
				logger.info("Claim error ==========> id: " + ex.getMessage());
				throw new Exception("This claim already exists.");
			}
			throw ex; // Rethrow if not related to constraint violation
		}
	}

	@Transactional
	public List<ClaimDto> getClaims(HttpServletRequest request) throws Exception {
		List<Claim> claims = claimRepository.findAll();

		return claims.stream().map(claim -> {
			ClaimDto claimDto = new ClaimDto();
			claimDto.setId(claim.getId());
			claimDto.setClaimDate(claim.getClaimDate());
			claimDto.setPaymentMethod(claim.getPaymentMethod());
			claimDto.setClaimNumber(claim.getClaimNumber());
			claimDto.setPolicyNumber(claim.getPolicyNumber());

			claimDto.setCompanyId(claim.getCompanyId());
			claimDto.setContactId(claim.getContactId());
			claimDto.setUpdatedBy(claim.getUpdatedBy());
			claimDto.setCreatedAt(claim.getCreatedAt());
			claimDto.setUpdatedAt(claim.getUpdatedAt());

			Set<ClaimDocumentDto> claimDocumentDtos = claim.getClaimDocuments().stream().map(document -> {
				ClaimDocumentDto documentDto = new ClaimDocumentDto();
				documentDto.setId(document.getId());
				documentDto.setFolderName(document.getFolderName());
				documentDto.setDocumentName(document.getDocumentName());
				documentDto.setBlobUrl(document.getBlobUrl());
				documentDto.setUpdatedBy(document.getUpdatedBy());
				documentDto.setCreatedAt(document.getCreatedAt());
				return documentDto;
			}).collect(Collectors.toSet());

			claimDto.setClaimDocuments(claimDocumentDtos);

			if (claim.getClaimMetadata() != null) {
				ClaimMetadataDto claimMetadataDto = new ClaimMetadataDto();
				claimMetadataDto.setId(claim.getClaimMetadata().getId());
				claimMetadataDto.setClaimId(claim.getId());
				claimMetadataDto.setMetadata(claim.getClaimMetadata().getMetadata());
				claimMetadataDto.setCreatedAt(claim.getClaimMetadata().getCreatedAt());
				claimMetadataDto.setUpdatedAt(claim.getClaimMetadata().getUpdatedAt());
				claimDto.setClaimMetadata(claimMetadataDto);
			}

			return claimDto;
		}).collect(Collectors.toList());
	}

}