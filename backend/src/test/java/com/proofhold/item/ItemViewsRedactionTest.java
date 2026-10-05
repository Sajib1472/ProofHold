package com.proofhold.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.proofhold.domain.ItemCategory;
import com.proofhold.domain.ItemStatus;
import com.proofhold.location.Location;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemViewsRedactionTest {

    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void publicJsonHidesPhotoSerialDescriptionAndAnswers() throws Exception {
        JsonNode json = mapper.valueToTree(ItemViews.toPublic(wallet(), 2));

        assertEquals("WALLET", json.get("category").asText());
        assertFalse(json.has("photoUrl"));
        assertFalse(json.has("serial"));
        assertFalse(json.has("uniqueMarks"));
        assertFalse(json.has("fullDescription"));
        assertFalse(json.has("challenges"));
        assertFalse(json.has("whereFound"));
        assertFalse(json.has("holdUntil"));
    }

    @Test
    void staffJsonIncludesPhotoSerialAndDescriptionButNotAnswerHashes() throws Exception {
        JsonNode json = mapper.valueToTree(ItemViews.toStaff(wallet(), secret(), List.of(challenge()), 2));

        assertEquals("https://desk.local/wallet.jpg", json.get("photoUrl").asText());
        assertEquals("WL-9", json.get("serial").asText());
        assertTrue(json.get("fullDescription").asText().contains("black leather"));
        assertEquals("What initials are inside?", json.get("challenges").get(0).get("prompt").asText());
        assertFalse(json.get("challenges").get(0).has("expectedAnswer"));
        assertFalse(json.get("challenges").get(0).has("expectedAnswerHash"));
    }

    static Item wallet() {
        Location desk = new Location();
        desk.setId(1L);
        desk.setName("Library Front Desk");
        desk.setTimezone("America/New_York");
        desk.setOpenFrom(LocalTime.of(9, 0));
        desk.setOpenTo(LocalTime.of(17, 0));
        Item item = new Item();
        item.setId(18L);
        item.setLocation(desk);
        item.setCategory(ItemCategory.WALLET);
        item.setStatus(ItemStatus.HELD);
        item.setFoundAt(Instant.parse("2026-10-05T14:00:00Z"));
        item.setHoldUntil(Instant.parse("2026-11-05T14:00:00Z"));
        item.setWhereFound("2nd floor");
        item.setVersion(0);
        return item;
    }

    static ItemSecret secret() {
        ItemSecret secret = new ItemSecret();
        secret.setPhotoUrl("https://desk.local/wallet.jpg");
        secret.setSerial("WL-9");
        secret.setUniqueMarks("initials JS");
        secret.setFullDescription("black leather wallet");
        return secret;
    }

    static Challenge challenge() {
        Challenge challenge = new Challenge();
        challenge.setId(3L);
        challenge.setPrompt("What initials are inside?");
        challenge.setExpectedAnswerHash("deadbeef");
        return challenge;
    }
}
