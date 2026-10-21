package com.campusgig.matching.domain;

import java.util.List;

/**
 * An improved job ad produced by the AI job ad assistant.
 *
 * @param improvedTitle       the rewritten title
 * @param improvedDescription the rewritten description
 * @param tips                what the assistant thought was weak in the employer's draft
 * @param revisions           how many times the assistant had to redo its rewrite after review
 * @param reviewNotes         problems the reviewer found in the assistant's rewrite (empty if none)
 */
public record JobAdReview(
        String improvedTitle,
        String improvedDescription,
        List<String> tips,
        int revisions,
        List<String> reviewNotes) {
}
