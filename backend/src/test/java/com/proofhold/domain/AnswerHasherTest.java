package com.proofhold.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AnswerHasherTest {

    @Test
    void normalizeCollapsesCaseAndSpace() {
        assertEquals("js", AnswerHasher.normalize("  JS  "));
        assertEquals(AnswerHasher.hash("JS"), AnswerHasher.hash(" js "));
        assertNotEquals(AnswerHasher.hash("JS"), AnswerHasher.hash("SJ"));
    }
}
