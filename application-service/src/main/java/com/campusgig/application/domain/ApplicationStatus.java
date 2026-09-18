package com.campusgig.application.domain;

/**
 * Lifecycle of a job application. The allowed transitions are a business rule,
 * so they live here in the domain rather than in the controller or service.
 */
public enum ApplicationStatus {
    SUBMITTED,
    SHORTLISTED,
    ACCEPTED,
    REJECTED,
    WITHDRAWN;

    public boolean canMoveTo(ApplicationStatus next) {
        return switch (this) {
            case SUBMITTED -> next == SHORTLISTED || next == ACCEPTED || next == REJECTED || next == WITHDRAWN;
            case SHORTLISTED -> next == ACCEPTED || next == REJECTED || next == WITHDRAWN;
            case ACCEPTED, REJECTED, WITHDRAWN -> false;
        };
    }
}
