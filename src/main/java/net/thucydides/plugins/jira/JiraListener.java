package net.thucydides.plugins.jira;

import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.model.ReportNamer.ReportType;
import net.thucydides.core.model.Stories;
import net.thucydides.core.model.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.steps.StepListener;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.plugins.jira.guice.Injectors;
import net.thucydides.plugins.jira.model.IssueComment;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.service.JIRAConfiguration;
import net.thucydides.plugins.jira.service.NoSuchIssueException;
import net.thucydides.plugins.jira.workflow.ClasspathWorkflowLoader;
import net.thucydides.plugins.jira.workflow.Workflow;
import net.thucydides.plugins.jira.workflow.WorkflowLoader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Updates JIRA issues referenced in a story with a link to the corresponding story report.
 */
public class JiraListener implements StepListener {

    private final IssueTracker issueTracker;

    private Class<?> currentTestCase;
    public Story currentStory;

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraListener.class);
    private final JIRAConfiguration configuration;
    private Workflow workflow;
    WorkflowLoader loader;

    private final EnvironmentVariables environmentVariables;

    public JiraListener(IssueTracker issueTracker,
                        EnvironmentVariables environmentVariables,
                        WorkflowLoader loader) {
        this.issueTracker = issueTracker;
        this.environmentVariables = environmentVariables;
        configuration = Injectors.getInjector().getInstance(JIRAConfiguration.class);
        this.loader = loader;
        workflow = loader.load();
    }

    protected boolean shouldUpdateIssues() {

        String jiraUrl = environmentVariables.getProperty(ThucydidesSystemProperty.JIRA_URL.getPropertyName());
        String reportUrl = environmentVariables.getProperty(ThucydidesSystemProperty.PUBLIC_URL.getPropertyName());
        LOGGER.info("JIRA LISTENER STATUS");
        LOGGER.info("JIRA URL = {} ", jiraUrl);
        LOGGER.info("REPORT URL = {} ", reportUrl);
        LOGGER.info("WORKFLOW ACTIVE = {} ", workflow.isActive());

        return !(StringUtils.isEmpty(jiraUrl) || StringUtils.isEmpty(reportUrl));
    }

    protected boolean shouldUpdateWorkflow() {
        Boolean workflowUpdatesEnabled
                = Boolean.valueOf(environmentVariables.getProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY));
        return (workflowUpdatesEnabled);
    }

    public JiraListener() {
        this(Injectors.getInjector().getInstance(IssueTracker.class),
             Injectors.getInjector().getInstance(EnvironmentVariables.class),
             Injectors.getInjector().getInstance(WorkflowLoader.class));
    }

    protected IssueTracker getIssueTracker() {
        return issueTracker;
    }

    protected Workflow getWorkflow() {
        return workflow;
    }

    public void testSuiteStarted(final Class<?> testCase) {
        this.currentTestCase = testCase;
        this.currentStory = null;
    }

    public void testSuiteStarted(final Story story) {
        this.currentStory = story;
        this.currentTestCase = null;
    }

    public void testStarted(final String testName) {
    }


    public void testFinished(TestOutcome result) {
        if (shouldUpdateIssues()) {
            List<String> issues = stripInitialHashesFrom(issueReferencesIn(result));
            updateIssues(issues, result.getResult());
        }
    }

    private Set<String> issueReferencesIn(TestOutcome result) {
        return result.getIssues();
    }

    private void updateIssues(List<String> issues, TestResult testResult) {
        for (String issueId : issues) {
            logIssueTracking(issueId);
            if (!dryRun()) {
                updateIssue(testResult, issueId);
            }
        }
    }

    private void updateIssue(TestResult testResult, String issueId) {
        try {
            addOrUpdateCommentFor(issueId);
            if (getWorkflow().isActive() && shouldUpdateWorkflow()) {
                updateIssueStatusFor(issueId, testResult);
            }
        } catch (NoSuchIssueException e) {
            LOGGER.error("No JIRA issue found with ID {}", issueId);
        }
    }

    private void updateIssueStatusFor(final String issueId, final TestResult testResult) {
        LOGGER.info("Updating status for issue {} with test result {}", issueId, testResult);

        String currentStatus = issueTracker.getStatusFor(issueId);

        List<String> transitions = getWorkflow().getTransitions().forTestResult(testResult).whenIssueIs(currentStatus);
        LOGGER.info("Found transitions: {}", transitions);

        for(String transition : transitions) {
            issueTracker.doTransition(issueId, transition);
        }
    }

    private void addOrUpdateCommentFor(final String issueId) {
        LOGGER.info("Updating comments for issue {}", issueId);

        List<IssueComment> comments = issueTracker.getCommentsFor(issueId);
        IssueComment existingComment = findExistingCommentIn(comments);
        if (existingComment != null) {
            IssueComment updatedComment = new IssueComment(existingComment.getId(), linkToReport(),
                                                           existingComment.getAuthor());
            issueTracker.updateComment(updatedComment);
        } else {
            issueTracker.addComment(issueId, linkToReport());
        }

    }

    private IssueComment findExistingCommentIn(List<IssueComment> comments) {
        for (IssueComment comment : comments) {
            if (comment.getText().contains("Thucydides Test Results")) {
                return comment;
            }
        }
        return null;
    }

    private void logIssueTracking(final String issueId) {
        if (dryRun()) {
            LOGGER.info("--- DRY RUN ONLY: JIRA WILL NOT BE UPDATED ---");
        }
        LOGGER.info("Updating JIRA issue: " + issueId);
        LOGGER.info("JIRA server: " + issueTracker.toString());
    }

    private boolean dryRun() {
        return Boolean.valueOf(environmentVariables.getProperty("thucydides.skip.jira.updates"));
    }

    private String linkToReport() {
        String reportUrl = environmentVariables.getProperty(ThucydidesSystemProperty.PUBLIC_URL.getPropertyName());
        String reportName = Stories.reportFor(storyUnderTest(), ReportType.HTML);
        return formatTestResultsLink(reportUrl, reportName);
    }

    public String formatTestResultsLink(String reportUrl, String reportName) {
        if (isWikiRenderedActive()) {
            return "[Thucydides Test Results|" + reportUrl + "/" + reportName + "]";
        } else {
            return "Thucydides Test Results: " + reportUrl + "/" + reportName;
        }
    }

    private boolean isWikiRenderedActive() {
        return configuration.isWikiRenderedActive();
    }

    private Story storyUnderTest() {
        if (currentTestCase != null) {
            return Stories.findStoryFrom(currentTestCase);
        } else {
            return currentStory;
        }
    }

    private List<String> stripInitialHashesFrom(final Set<String> issueNumbers) {
        List<String> issues = new ArrayList<String>();
        if (issueNumbers != null) {
            for (String issueNumber : issueNumbers) {
                if (issueNumber.startsWith("#")) {
                    issues.add(issueNumber.substring(1));
                } else {
                    issues.add(issueNumber);
                }
            }
        }
        return issues;
    }

    public void stepStarted(ExecutedStepDescription executedStepDescription) {

    }

    public void stepFailed(StepFailure stepFailure) {

    }

    public void stepIgnored() {

    }

    public void stepPending() {

    }

    public void stepFinished() {

    }

    public void testFailed(Throwable throwable) {

    }

    public void testIgnored() {

    }
}
