package com.proofhold.claim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "claim_answers")
@IdClass(ClaimAnswer.Key.class)
public class ClaimAnswer {

    @Id
    @Column(name = "claim_id")
    private Long claimId;

    @Id
    @Column(name = "challenge_id")
    private Long challengeId;

    @Column(nullable = false)
    private String value;

    public Long getClaimId() {
        return claimId;
    }

    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }

    public Long getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Long challengeId) {
        this.challengeId = challengeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static class Key implements Serializable {
        private Long claimId;
        private Long challengeId;
    }
}
