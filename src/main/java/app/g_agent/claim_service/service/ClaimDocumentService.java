package app.g_agent.claim_service.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.g_agent.claim_service.dto.ClaimDocumentDto;
import app.g_agent.claim_service.model.ClaimDocument;
import app.g_agent.claim_service.repository.ClaimDocumentRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ClaimDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(ClaimService.class);

    private ClaimDocumentRepository claimDocumentRepository;
    private JwtService jwtService;

    public ClaimDocumentService(ClaimDocumentRepository claimDocumentRepository, JwtService jwtService) {
        this.claimDocumentRepository = claimDocumentRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public void createClaimDocument(HttpServletRequest request, ClaimDocumentDto claimDocumentDto) throws Exception {
        ClaimDocument claimDocument = new ClaimDocument();

        int userId = (int) jwtService.getTokenValue(jwtService.getJWT(request), "user-id");

        claimDocument.setFolderName(claimDocumentDto.getFolderName());
        claimDocument.setDocumentName(claimDocumentDto.getDocumentName());
        claimDocument.setBlobUrl(claimDocumentDto.getBlobUrl());
        claimDocument.setUpdatedBy(Long.valueOf(userId));

        try {
            claimDocumentRepository.save(claimDocument);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("ClaimDocument error ==========> id: " + ex.getMessage());
                throw new Exception("This claim document already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public void updateClaimDocument(HttpServletRequest request, ClaimDocumentDto claimDocumentDto, Long id) throws Exception {
        Optional<ClaimDocument> claimDocumentOpt = claimDocumentRepository.findById(id);

        if (claimDocumentOpt.isEmpty()) {
            throw new Exception("The claim document cannot be found");
        }

        ClaimDocument claimDocument = claimDocumentOpt.get();

        int userId = (int) jwtService.getTokenValue(jwtService.getJWT(request), "user-id");

        claimDocument.setFolderName(claimDocumentDto.getFolderName());
        claimDocument.setDocumentName(claimDocumentDto.getDocumentName());
        claimDocument.setBlobUrl(claimDocumentDto.getBlobUrl());
        claimDocument.setUpdatedBy(Long.valueOf(userId));

        try {
            claimDocumentRepository.save(claimDocument);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
                logger.info("ClaimDocument error ==========> id: " + ex.getMessage());
                throw new Exception("This claim document already exists.");
            }
            throw ex; // Rethrow if not related to constraint violation
        }
    }

    @Transactional
    public void deleteClaimDocument(HttpServletRequest request, Long id) throws Exception {
        Optional<ClaimDocument> claimDocumentOpt = claimDocumentRepository.findById(id);

        if (claimDocumentOpt.isPresent()) {
            claimDocumentRepository.delete(claimDocumentOpt.get());
        } else {
            throw new Exception("The claim document cannot be found");
        }
    }

    public ClaimDocumentDto getClaimDocument(HttpServletRequest request, Long id) throws Exception {
        Optional<ClaimDocument> claimDocumentOpt = claimDocumentRepository.findById(id);

        if (claimDocumentOpt.isPresent()) {
            ClaimDocumentDto claimDocumentDto = new ClaimDocumentDto();
            claimDocumentDto.setId(claimDocumentOpt.get().getId());
            claimDocumentDto.setClaimId(claimDocumentOpt.get().getClaim().getId());
            claimDocumentDto.setFolderName(claimDocumentOpt.get().getFolderName());
            claimDocumentDto.setDocumentName(claimDocumentOpt.get().getDocumentName());
            claimDocumentDto.setBlobUrl(claimDocumentOpt.get().getBlobUrl());
            claimDocumentDto.setUpdatedBy(claimDocumentOpt.get().getUpdatedBy());
            claimDocumentDto.setCreatedAt(claimDocumentOpt.get().getCreatedAt());

            return claimDocumentDto;
        } else {
            throw new Exception("The claim document does not exist");
        }
    }

    public List<ClaimDocumentDto> getClaimDocuments(HttpServletRequest request) throws Exception {
        List<ClaimDocument> claimDocuments = claimDocumentRepository.findAll();

        return claimDocuments.stream().map(claimDocument -> {
            ClaimDocumentDto claimDocumentDto = new ClaimDocumentDto();
            claimDocumentDto.setId(claimDocument.getId());
            claimDocumentDto.setClaimId(claimDocument.getClaim().getId());
            claimDocumentDto.setFolderName(claimDocument.getFolderName());
            claimDocumentDto.setDocumentName(claimDocument.getDocumentName());
            claimDocumentDto.setBlobUrl(claimDocument.getBlobUrl());
            claimDocumentDto.setUpdatedBy(claimDocument.getUpdatedBy());
            claimDocumentDto.setCreatedAt(claimDocument.getCreatedAt());
            return claimDocumentDto;
        }).collect(Collectors.toList());
    }
}