package app.g_agent.claim_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.g_agent.claim_service.commons.Message;
import app.g_agent.claim_service.dto.ClaimDocumentDto;
import app.g_agent.claim_service.service.ClaimDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/claim-document")
@Validated
public class ClaimDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(ClaimDocumentController.class);

    @Autowired
    ClaimDocumentService claimDocumentService;

    @PostMapping("/create")
    public ResponseEntity<?> createClaimDocument(HttpServletRequest request,
            @Valid @RequestBody ClaimDocumentDto claimDocumentDto) {
        Message message = new Message();

        try {
            claimDocumentService.createClaimDocument(request, claimDocumentDto);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Claim document created successfully");
        return ResponseEntity.ok(message);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateClaimDocument(HttpServletRequest request,
            @RequestBody ClaimDocumentDto claimDocumentDto,
            @RequestParam Long id) {
        Message message = new Message();

        try {
            claimDocumentService.updateClaimDocument(request, claimDocumentDto, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Claim document updated successfully");
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteClaimDocument(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            claimDocumentService.deleteClaimDocument(request, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Claim document deleted successfully");
        return ResponseEntity.ok(message);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getClaimDocument(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(claimDocumentService.getClaimDocument(request, id));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }

    @GetMapping("/get-claim-documents")
    public ResponseEntity<?> getClaimDocuments(HttpServletRequest request) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(claimDocumentService.getClaimDocuments(request));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }
}