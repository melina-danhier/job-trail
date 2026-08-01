package com.melina.jobtrail.exception;

public class CompanyHasApplicationsException extends RuntimeException {
    public CompanyHasApplicationsException(long companyId) {
        super("Company with id " + companyId + " cannot be deleted while applications reference it");
    }
}
