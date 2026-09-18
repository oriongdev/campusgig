package com.campusgig.job.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobTest {

    private Job barista() {
        return new Job(1L, " Weekend Barista ", "Coffee and customer service", "Hospitality", "Wollongong",
                new Pay(new BigDecimal("28.50"), PayType.HOURLY));
    }

    @Test
    void newJobIsOpenAndNormalised() {
        Job job = barista();
        assertTrue(job.isOpen());
        assertEquals("Weekend Barista", job.getTitle());
        assertEquals("hospitality", job.getCategory());
    }

    @Test
    void searchMatchesOnCategoryLocationAndPay() {
        Job job = barista();
        assertTrue(job.matches("hospitality", "woll", new BigDecimal("25")));
        assertTrue(job.matches(null, null, null));
        assertEquals(false, job.matches("tutoring", null, null));
        assertEquals(false, job.matches(null, null, new BigDecimal("30")));
    }

    @Test
    void jobCanBeClosedOnlyOnce() {
        Job job = barista();
        job.close();
        assertEquals(JobStatus.CLOSED, job.getStatus());
        assertThrows(IllegalStateException.class, job::close);
    }

    @Test
    void openJobCanBeEdited() {
        Job job = barista();
        job.update("Weekend Barista (2 shifts)", "Saturday and Sunday mornings", "Hospitality", "Wollongong",
                new Pay(new BigDecimal("30.00"), PayType.HOURLY));
        assertEquals("Weekend Barista (2 shifts)", job.getTitle());
        assertEquals(new BigDecimal("30.00"), job.getPay().amount());
    }

    @Test
    void closedJobCannotBeEdited() {
        Job job = barista();
        job.close();
        assertThrows(IllegalStateException.class, () -> job.update("New title", "", "Hospitality", "Wollongong",
                new Pay(new BigDecimal("30.00"), PayType.HOURLY)));
    }

    @Test
    void payMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Pay(new BigDecimal("0"), PayType.HOURLY));
        assertThrows(IllegalArgumentException.class, () -> new Pay(new BigDecimal("-5"), PayType.FIXED));
    }

    @Test
    void requiredFieldsAreValidated() {
        Pay pay = new Pay(new BigDecimal("20"), PayType.HOURLY);
        assertThrows(IllegalArgumentException.class, () -> new Job(null, "t", "", "c", "l", pay));
        assertThrows(IllegalArgumentException.class, () -> new Job(1L, " ", "", "c", "l", pay));
        assertThrows(IllegalArgumentException.class, () -> new Job(1L, "t", "", " ", "l", pay));
    }
}
