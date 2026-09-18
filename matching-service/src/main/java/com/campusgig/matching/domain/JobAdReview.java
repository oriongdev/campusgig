package com.campusgig.matching.domain;

import java.util.List;

/** An improved job ad produced by the AI job ad assistant. */
public record JobAdReview(String improvedTitle, String improvedDescription, List<String> tips) {
}
