package net.thucydides.plugins.jira.service;

/**
 * Thrown when the issue tracking system could not be updated for some reason.
 */
public class IssueTrackerUpdateException extends RuntimeException{
    public IssueTrackerUpdateException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
