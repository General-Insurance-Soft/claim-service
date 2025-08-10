package app.g_agent.claim_service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ClaimDto {

    private Long id;

    @NotBlank(message = "claim_number is required")
    @JsonProperty("claim_number")
    String claimNumber;

    @NotBlank(message = "policy_number is required")
    @JsonProperty("policy_number")
    String policyNumber;

    @NotNull(message = "payment_method is required")
    @JsonProperty("payment_method")
    private Long paymentMethod;

    @JsonProperty("claim_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate claimDate;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("updated_by")
    private Long updatedBy;

    @JsonProperty("company_id")
    private Long companyId;

    @NotNull(message = "Contact ID is required")
    @JsonProperty("contact_id")
    private Long contactId;

    @JsonProperty("claim_documents")
    private Set<ClaimDocumentDto> claimDocuments;

    @JsonProperty("claim_metadata")
    private ClaimMetadataDto claimMetadata;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setClaimDate(LocalDate claimDate) {
        this.claimDate = claimDate;
    }

    public LocalDate getClaimDate() {
        return claimDate;
    }

    public Long getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Long paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Set<ClaimDocumentDto> getClaimDocuments() {
        return claimDocuments;
    }

    public void setClaimDocuments(Set<ClaimDocumentDto> claimDocuments) {
        this.claimDocuments = claimDocuments;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public ClaimMetadataDto getClaimMetadata() {
        return claimMetadata;
    }

    public void setClaimMetadata(ClaimMetadataDto claimMetadata) {
        this.claimMetadata = claimMetadata;
    }
}