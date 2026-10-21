package com.campusgig.matching.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain service: rule-based checks on an AI-rewritten job ad.
 *
 * <p>The business rule is that the assistant may improve wording but must never change what the
 * employer is offering. An LLM reviewer checks that too, but the rule about money is too important
 * to leave to a model alone, so it is also enforced here in plain code.
 */
public class JobAdGuard {

    private static final Pattern DOLLAR_AMOUNT = Pattern.compile("\\$\\s?(\\d+(?:[.,]\\d+)*)");
    private static final Pattern ANY_NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    /**
     * @param originalText every part of the employer's draft, including the pay field
     * @return a list of problems, empty when the rewrite passes
     */
    public List<String> check(String originalText, String improvedTitle, String improvedDescription) {
        List<String> problems = new ArrayList<>();
        if (improvedDescription == null || improvedDescription.isBlank()) {
            problems.add("The rewritten ad has no description.");
        }
        Set<BigDecimal> allowed = numbersIn(originalText, ANY_NUMBER);
        String improved = safe(improvedTitle) + " " + safe(improvedDescription);
        for (BigDecimal amount : numbersIn(improved, DOLLAR_AMOUNT)) {
            boolean known = allowed.stream().anyMatch(original -> original.compareTo(amount) == 0);
            if (!known) {
                problems.add("The rewritten ad mentions $" + amount.toPlainString()
                        + ", which is not in the employer's draft. Do not add or change pay amounts.");
            }
        }
        return problems;
    }

    private static Set<BigDecimal> numbersIn(String text, Pattern pattern) {
        Set<BigDecimal> numbers = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(safe(text));
        while (matcher.find()) {
            try {
                numbers.add(new BigDecimal(matcher.group(1).replace(",", "")));
            } catch (NumberFormatException ignored) {
                // not a usable number, e.g. "1.2.3"
            }
        }
        return numbers;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
