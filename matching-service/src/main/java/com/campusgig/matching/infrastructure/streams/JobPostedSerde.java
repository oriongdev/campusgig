package com.campusgig.matching.infrastructure.streams;

import com.campusgig.matching.domain.event.JobPosted;

public class JobPostedSerde extends JsonSerde<JobPosted> {

    public JobPostedSerde() {
        super(JobPosted.class);
    }
}
