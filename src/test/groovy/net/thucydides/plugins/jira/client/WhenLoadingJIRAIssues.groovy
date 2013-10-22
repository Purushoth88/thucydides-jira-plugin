package net.thucydides.plugins.jira.client

import com.google.common.base.Optional
import net.thucydides.plugins.jira.domain.IssueSummary
import spock.lang.Specification

class WhenLoadingJIRAIssues extends Specification {

    def "should load issue keys with JQL filters"() {
        given:
        def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile")
        when:
        List<IssueSummary> issues = jiraClient.findByJQL("project='DEMO'")
        then:
        issues.size() > 10
    }

    def "should load issue summary by key"() {
        given:
        def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile")
        when:
        Optional<IssueSummary> issue = jiraClient.findByKey("DEMO-8")
        then:
        issue.isPresent()
        and:
        issue.get().key == "DEMO-8"
    }


    def "should load rendered descriptions"() {
        given:
        def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile")
        when:
        Optional<IssueSummary> issue = jiraClient.findByKey("TRAD-8")
        then:
        issue.isPresent()
        and:
        issue.get().renderedDescription.contains("<h2>")
    }

    def "should not load issue by key if the issue is not available"() {
        given:
        def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile")
        when:
        Optional<IssueSummary> issue = jiraClient.findByKey("DEMO-DOES-NOT-EXIST")
        then:
        !issue.isPresent()
    }

    def "should load the fix versions for an issue "() {
        given:
        def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile")
        when:
        Optional<IssueSummary> issue = jiraClient.findByKey("DEMO-2")
        then:
        issue.get().fixVersions.size() == 2
        and:
        issue.get().fixVersions.contains("Version 1.0")
        and:
        issue.get().fixVersions.contains("Iteration 1.1")
    }

    def "should not freak out if a JQL query doesn't return any issues"() {
        given:
        def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile")
        when:
        List<IssueSummary> issue = jiraClient.findByJQL("key=DEMO-DOES-NOT-EXIST")
        then:
        issue.isEmpty()
    }

    def "should not freak out if a JQL count doesn't return any issues"() {
        given:
        def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile")
        when:
        def total = jiraClient.countByJQL("key=DEMO-DOES-NOT-EXIST")
        then:
        total == 0
    }

    InputStream streamed(String source) { new ByteArrayInputStream(source.bytes) }
}
