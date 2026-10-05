package com.proofhold.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.proofhold.claim.ClaimRepository;
import com.proofhold.domain.ClaimStatus;
import com.proofhold.domain.ItemCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceSearchTest {

    @Mock
    ItemRepository items;
    @Mock
    ClaimRepository claims;
    @Mock
    ItemSecretRepository secrets;
    @Mock
    ChallengeRepository challenges;
    @Mock
    com.proofhold.location.LocationRepository locations;
    @Mock
    com.proofhold.user.UserRepository users;
    @Mock
    com.proofhold.audit.AuditService audit;

    @InjectMocks
    ItemService service;

    @Test
    void searchReturnsPaginationAndRedactedPublicItems() throws Exception {
        Item wallet = ItemViewsRedactionTest.wallet();
        when(items.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(wallet), PageRequest.of(0, 20), 1));
        when(claims.countByItemIdAndStatus(18L, ClaimStatus.PENDING)).thenReturn(2L);

        PublicItemPage page = service.list(null, ItemCategory.WALLET, null, null, 0, 20);
        JsonNode json = JsonMapper.builder().findAndAddModules().build().valueToTree(page);

        assertEquals(1, page.page().totalElements());
        assertEquals(0, page.page().page());
        assertEquals(20, page.page().size());
        assertEquals(1, page.page().totalPages());
        JsonNode item = json.get("content").get(0);
        assertFalse(item.has("photoUrl"));
        assertFalse(item.has("serial"));
        assertFalse(item.has("fullDescription"));
        assertFalse(item.has("challenges"));
    }

    @Test
    void filterByCategoryIsPassedToSpecification() {
        when(items.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.list(null, ItemCategory.WALLET, null, "library", 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(items).findAll(any(Specification.class), pageable.capture());
        assertEquals("foundAt: DESC,id: DESC", pageable.getValue().getSort().toString());
        verify(items).findAll(any(Specification.class), eq(pageable.getValue()));
    }
}
