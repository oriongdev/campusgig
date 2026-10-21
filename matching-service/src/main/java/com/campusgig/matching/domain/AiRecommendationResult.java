package com.campusgig.matching.domain;

import java.util.List;

/**
 * The answer to "which jobs suit this student?", together with where the answer came from.
 *
 * <p>Normally the source is the AI matching agent. If the LLM cannot be reached or replies with
 * something unusable, the service falls back to the rule-based {@link SkillMatcher} and says so,
 * so the student still gets recommendations and the UI can show what happened.
 */
public record AiRecommendationResult(String source, String note, List<AiRecommendation> recommendations) {

    public static final String SOURCE_AI_AGENT = "AI_AGENT";
    public static final String SOURCE_RULE_BASED_FALLBACK = "RULE_BASED_FALLBACK";

    public static AiRecommendationResult fromAgent(List<AiRecommendation> recommendations) {
        return new AiRecommendationResult(SOURCE_AI_AGENT, null, List.copyOf(recommendations));
    }

    public static AiRecommendationResult fallback(String note, List<AiRecommendation> recommendations) {
        return new AiRecommendationResult(SOURCE_RULE_BASED_FALLBACK, note, List.copyOf(recommendations));
    }
}
