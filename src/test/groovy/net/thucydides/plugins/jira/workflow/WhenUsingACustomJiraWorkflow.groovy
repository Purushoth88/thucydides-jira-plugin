package net.thucydides.plugins.jira.workflow

import net.thucydides.core.util.MockEnvironmentVariables
import net.thucydides.plugins.jira.guice.Injectors
import spock.lang.Shared
import spock.lang.Specification

import static net.thucydides.core.model.TestResult.FAILURE
import static net.thucydides.core.model.TestResult.SUCCESS

class WhenUsingACustomJiraWorkflow extends Specification {

    def workflow

    @Shared
    def environmentVariables = new MockEnvironmentVariables()

    def setupSpec() {
        environmentVariables.setProperty('thucydides.jira.workflow','custom-workflow.groovy')
    }

    def setup() {
        workflow = new ClasspathWorkflowLoader("jira-workflow.groovy", environmentVariables).load();
    }

    def "should load a custom workflow defined in the thucydides.jira.workflow system property"() {

        expect:
        def transitions = workflow.transitions.forTestResult(result).whenIssueIs(issueStatus)
        transitions == expectedTransitions

        where:
        issueStatus          | result  | expectedTransitions
        'Open'               | SUCCESS | ['Resolve Issue']
        'Pending Validation' | FAILURE | ['Reopen Issue']
    }

}