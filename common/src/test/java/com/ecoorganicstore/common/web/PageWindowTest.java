package com.ecoorganicstore.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageWindowTest {
    @Test
    void clampsPageAndSizeSoAClientCannotRequestAnUnboundedRead() {
        assertEquals(0, PageWindow.page(-4));
        assertEquals(PageWindow.MAX_PAGE, PageWindow.page(9_000));
        assertEquals(12, PageWindow.size(0, 12));
        assertEquals(PageWindow.MAX_SIZE, PageWindow.size(10_000, 12));
    }

    @Test
    void pageReportsWhetherAnotherSliceExists() {
        PageResponse<String> first = PageResponse.of(List.of("a"), 0, 1, 3);
        PageResponse<String> last = PageResponse.of(List.of("c"), 2, 1, 3);

        assertEquals(3, first.totalPages());
        assertTrue(first.hasNext());
        assertFalse(last.hasNext());
    }

    @Test
    void searchTextIsClippedAndRegexMetaIsEscaped() {
        assertEquals("", SearchText.literal("   "));
        assertEquals("a\\.b", SearchText.literal("a.b"));
        assertEquals(SearchText.MAX_LENGTH, SearchText.clip("x".repeat(200)).length());
    }
}
