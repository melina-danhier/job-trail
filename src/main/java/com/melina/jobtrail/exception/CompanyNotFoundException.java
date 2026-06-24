package com.melina.jobtrail.exception;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(String companyName) {
        super("Company with name " + companyName + " not found");
    }
    public CompanyNotFoundException(long id) {
        super("Company with id " + id + " not found");
    }
}
