package app.g_agent.claim_service.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import app.g_agent.claim_service.dto.ClaimDocumentDto;
import app.g_agent.claim_service.dto.ClaimDto;
import app.g_agent.claim_service.dto.ClaimMetadataDto;
import app.g_agent.claim_service.dto.ContactAddressWrapper;
import app.g_agent.claim_service.dto.UserDto;
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

	@Autowired
	private ContactsDataClient contactsDataClient;
	@Autowired
	private UsersDataClient usersDataClient;

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

		Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

		Optional<Claim> claimOpt = claimRepository.findByIdAndCompanyId(id, orgId);

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
			claimDto.setClaimStatus(claim.getClaimStatus());

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
		claim.setClaimStatus(claimDto.getClaimStatus());

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

		Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
		Optional<Claim> claimOpt = claimRepository.findByIdAndCompanyId(id, orgId);

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
		claim.setClaimStatus(claimDto.getClaimStatus());

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

	public List<ClaimDto> getClaimByContact(HttpServletRequest request, Long id) throws Exception {

		Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());

		List<Claim> claimPage = claimRepository.findByContactIdAndCompanyId(id, orgId);

		List<ClaimDto> claims = claimPage.stream().map(claim -> {

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
			claimDto.setClaimStatus(claim.getClaimStatus());

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

			return claimDto;
		}).collect(Collectors.toList());

		return claims;

	}

	@Transactional
	public Map<String, Object> getClaims(HttpServletRequest request, MultiValueMap<String, String> headers, int page,
			int size) throws Exception {

		// List<Proposal> proposals = policyRepository.findAll();
		Long orgId = Long.parseLong(jwtService.getTokenValue(jwtService.getJWT(request), "organization-id").toString());
		Pageable pageable = PageRequest.of(page, size, Sort.by("created_at").descending());
		Page<Object[]> claimPage = claimRepository.findLatestClaimsPerContact(pageable, orgId);

		Set<Long> contacts = new HashSet<Long>();
		Set<Long> updatedByUsers = new HashSet<Long>();

		Set<Long> claimIds = new HashSet<>();
		List<ClaimDto> dtoList = new ArrayList<>();

		for (Object[] row : claimPage.getContent()) {
			// ClaimDto claimDto = new ClaimDto();
			logger.info("Mapping claim row: " + Arrays.toString(row));

			// [2025-08-10, 2, 7, 2025-08-10 20:38:52.244017, 3, 1, 2025-08-10
			// 20:38:52.244017, 2, claim rer3, dsdsd, 1, 3]
			ClaimDto claimDto = new ClaimDto();
			claimDto.setId((Long) row[4]);
			claimIds.add(claimDto.getId());
			claimDto.setClaimDate(((java.sql.Date) row[0]).toLocalDate());
			claimDto.setPaymentMethod((Long) row[5]);
			claimDto.setClaimNumber((String) row[8]);
			claimDto.setPolicyNumber((String) row[9]);

			claimDto.setCompanyId((Long) row[1]);
			claimDto.setContactId((Long) row[2]);
			claimDto.setUpdatedBy((Long) row[7]);
			//claimStatus
			claimDto.setClaimStatus((String) row[10]);
			claimDto.setCreatedAt(((java.sql.Timestamp) row[3]).toLocalDateTime());
			claimDto.setUpdatedAt(((java.sql.Timestamp) row[6]).toLocalDateTime());

			contacts.add((Long) row[2]);
			updatedByUsers.add((Long) row[7]);
			Long claimCount = ((Number) row[row.length - 1]).longValue(); // last element
			claimDto.setClaimCount(claimCount);

			dtoList.add(claimDto);
		}
		// start fetch documents
		List<Claim> claimsWithDocs = claimRepository.findAllWithDocumentsByIds(claimIds);

		Map<Long, Set<ClaimDocumentDto>> claimDocsMap = claimsWithDocs.stream()
				.collect(Collectors.toMap(
						Claim::getId,
						claim -> claim.getClaimDocuments().stream().map(document -> {
							ClaimDocumentDto docDto = new ClaimDocumentDto();
							docDto.setId(document.getId());
							docDto.setFolderName(document.getFolderName());
							docDto.setDocumentName(document.getDocumentName());
							docDto.setBlobUrl(document.getBlobUrl());
							docDto.setUpdatedBy(document.getUpdatedBy());
							docDto.setCreatedAt(document.getCreatedAt());
							return docDto;
						}).collect(Collectors.toSet())));

		dtoList.forEach(dto -> {
			dto.setClaimDocuments(claimDocsMap.getOrDefault(dto.getId(), Collections.emptySet()));
		});
		// end fetch documents

		Page<ClaimDto> claims = new PageImpl<>(dtoList, claimPage.getPageable(),
				claimPage.getTotalElements());

		ContactAddressWrapper contactsData = getAdministrativeAreasData(contacts, headers);
		List<UserDto> updatedByUsersData = getUpdatedByData(updatedByUsers, headers);

		Map<String, Object> response = new HashMap<>();

		response.put("totalElements", claims.getTotalElements());
		response.put("totalPages", claims.getTotalPages());
		response.put("currentPage", claims.getNumber());

		response.put("claims", claims.getContent());
		response.put("contact", contactsData.contacts);
		response.put("updatedBy", updatedByUsersData);
		response.put("administrative_areas", contactsData.localityMapper);

		return response;

	}

	private ContactAddressWrapper getAdministrativeAreasData(Set<Long> ids,
			MultiValueMap<String, String> headers) {
		logger.info("Number of admin area IDs============>: {}", ids.size());

		headers.add("Content-Type", "application/json");

		Map<String, String> flattenedHeaders = new HashMap<>();
		headers.forEach((key, values) -> {
			flattenedHeaders.put(key, String.join(",", values));
		});

		try {
			String contactIds = this.getStringFromList(ids);

			logger.info("Request body as a string ============>: {}", ids);

			ContactAddressWrapper results = contactsDataClient.getContactsByIds(contactIds, flattenedHeaders);

			return results;

		} catch (Exception e) {
			logger.error("Error occurred while fetching administrative areas: {}",
					e.getMessage());
			ContactAddressWrapper results = new ContactAddressWrapper();
			return results;
		}

	}

	private List<UserDto> getUpdatedByData(Set<Long> ids,
			MultiValueMap<String, String> headers) {
		logger.info("Get update by IDs, size is============>: {}", ids.size());

		headers.add("Content-Type", "application/json");

		Map<String, String> flattenedHeaders = new HashMap<>();
		headers.forEach((key, values) -> {
			flattenedHeaders.put(key, String.join(",", values));
		});

		try {
			String stringIds = this.getStringFromList(ids);

			logger.info("Request body as a string ============>: {}", stringIds);

			List<UserDto> results = usersDataClient.getUsersByIds(stringIds, flattenedHeaders);

			return results;

		} catch (Exception e) {
			logger.error("Error occurred while fetching users: {}",
					e.getMessage());
			List<UserDto> results = new ArrayList<>();
			return results;
		}

	}

	private String getStringFromList(Set<Long> ids) {
		return ids.stream()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
	}
}