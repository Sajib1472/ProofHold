package com.proofhold.item;

import com.proofhold.domain.ItemStatus;
import com.proofhold.location.Location;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public final class ItemViews {

    private ItemViews() {}

    public static PublicItemResponse toPublic(Item item, int pendingClaimCount) {
        Location location = item.getLocation();
        return new PublicItemResponse(
                item.getId(),
                location.getId(),
                location.getName(),
                item.getCategory(),
                foundOn(item, location),
                item.getStatus(),
                pendingClaimCount,
                version(item));
    }

    public static StaffItemResponse toStaff(
            Item item, ItemSecret secret, List<Challenge> challenges, int pendingClaimCount) {
        Location location = item.getLocation();
        return new StaffItemResponse(
                item.getId(),
                location.getId(),
                location.getName(),
                item.getCategory(),
                foundOn(item, location),
                item.getStatus(),
                pendingClaimCount,
                version(item),
                item.getHoldUntil(),
                item.getWhereFound(),
                secret == null ? null : secret.getPhotoUrl(),
                secret == null ? null : secret.getSerial(),
                secret == null ? null : secret.getUniqueMarks(),
                secret == null ? null : secret.getFullDescription(),
                challenges.stream()
                        .map(c -> new StaffChallengeResponse(c.getId(), c.getPrompt()))
                        .toList());
    }

    public static VerifiedClaimerItemResponse toVerifiedClaimer(Item item, ItemSecret secret, int pendingClaimCount) {
        Location location = item.getLocation();
        return new VerifiedClaimerItemResponse(
                item.getId(),
                location.getId(),
                location.getName(),
                item.getCategory(),
                foundOn(item, location),
                item.getStatus(),
                pendingClaimCount,
                version(item),
                secret == null ? null : secret.getPhotoUrl(),
                secret == null ? null : secret.getSerial(),
                secret == null ? null : secret.getUniqueMarks(),
                secret == null ? null : secret.getFullDescription());
    }

    public static boolean isClaimable(ItemStatus status) {
        return status == ItemStatus.HELD || status == ItemStatus.CLAIM_PENDING;
    }

    private static LocalDate foundOn(Item item, Location location) {
        return item.getFoundAt().atZone(ZoneId.of(location.getTimezone())).toLocalDate();
    }

    private static int version(Item item) {
        return item.getVersion() == null ? 0 : item.getVersion();
    }
}
