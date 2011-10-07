package net.thucydides.plugins.jira.model;

import java.util.List;

/**
 * An interface to an issue tracking system.
 * Should allow a client to connect to an issue tracking system, retrieve comments for an existing issue, and
 * add new comments.
 */
public interface IssueTracker {
    /**
     * Add a new comment to the specified issue in the remote issue tracking system.
     * @param issueKey the unique key identifying the issue to be commented.
     * @param commentText  text of the comment.
     */
    void addComment(final String issueKey, final String commentText) throws IssueTrackerUpdateException;

    List<IssueComment> getCommentsFor(final String issueKey) throws IssueTrackerUpdateException;

    void updateComment(final IssueComment issueComment);

    public String getStatusFor(final String issueKey) throws IssueTrackerUpdateException;

    public void updateStatus(final String issueKey, final String status) throws IssueTrackerUpdateException;

}
