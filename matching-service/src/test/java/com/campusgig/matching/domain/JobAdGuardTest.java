package com.campusgig.matching.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobAdGuardTest {

    private static final String DRAFT = "Weekend Barista hospitality Wollongong 28.50 HOURLY make coffee";

    private final JobAdGuard guard = new JobAdGuard();

    @Test
    void aRewriteThatKeepsThePayPasses() {
        List<String> problems = guard.check(DRAFT, "Weekend Barista", "Join our cafe team for $28.50 an hour.");

        assertTrue(problems.isEmpty());
    }

    @Test
    void theSameAmountWrittenDifferentlyStillPasses() {
        assertTrue(guard.check(DRAFT, "Weekend Barista ($28.5/hr)", "Make great coffee.").isEmpty());
    }

    @Test
    void aChangedPayAmountIsFlagged() {
        List<String> problems = guard.check(DRAFT, "Weekend Barista", "Earn $35 an hour making coffee.");

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("$35"));
    }

    @Test
    void anInventedBonusIsFlagged() {
        List<String> problems = guard.check(DRAFT, "Weekend Barista",
                "Earn $28.50 an hour plus a $500 sign-on bonus.");

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("$500"));
    }

    @Test
    void anEmptyDescriptionIsFlagged() {
        List<String> problems = guard.check(DRAFT, "Weekend Barista", "  ");

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("no description"));
    }

    @Test
    void amountsWithThousandsSeparatorsAreUnderstood() {
        assertTrue(guard.check("Event crew, fixed pay 1200", "Event crew", "A fixed $1,200 for the weekend.").isEmpty());
    }
}
