package com.campusgig.matching.infrastructure.streams;

import com.campusgig.matching.domain.event.ApplicationSubmitted;

public class ApplicationSubmittedSerde extends JsonSerde<ApplicationSubmitted> {

    public ApplicationSubmittedSerde() {
        super(ApplicationSubmitted.class);
    }
}
