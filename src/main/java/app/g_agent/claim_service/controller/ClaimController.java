package app.g_agent.claim_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.g_agent.claim_service.commons.Message;
import app.g_agent.claim_service.dto.ClaimDto;
import app.g_agent.claim_service.service.ClaimService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/claim")
@Validated
public class ClaimController {

    private static final Logger logger = LoggerFactory.getLogger(ClaimController.class);

    @Autowired
    ClaimService claimService;

    @PostMapping("/create")
    public ResponseEntity<?> createClaim(HttpServletRequest request, @Valid @RequestBody ClaimDto claimDto) {
        Message message = new Message();

        try {
            claimService.createClaim(request, claimDto);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Claim created successfully");
        return ResponseEntity.ok(message);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateClaim(HttpServletRequest request, @RequestBody ClaimDto claimDto,
            @RequestParam Long id) {
        Message message = new Message();

        try {
            claimService.updateClaim(request, claimDto, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Claim updated successfully");
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteClaim(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            claimService.deleteClaim(request, id);
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
        message.setName("Success");
        message.setMessage("Claim deleted successfully");
        return ResponseEntity.ok(message);
    }

    @GetMapping("/get")
    public ResponseEntity<?> getClaim(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(claimService.getClaimById(request, id));
        } catch (Exception ex) {
            message.setName("Error");
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }

    @GetMapping("/get-claims")
    public ResponseEntity<?> getClaims(
            HttpServletRequest request,
            @RequestHeader MultiValueMap<String, String> headers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(claimService.getClaims(request, headers, page, size));
        } catch (Exception ex) {
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }

    @GetMapping("/get-by-contact")
    public ResponseEntity<?> getClaimByContact(HttpServletRequest request, @RequestParam Long id) {
        Message message = new Message();

        try {
            return ResponseEntity.ok(claimService.getClaimByContact(request, id));
        } catch (Exception ex) {
            message.setMessage(ex.getMessage());
            return ResponseEntity.status(403).body(message);
        }
    }
}