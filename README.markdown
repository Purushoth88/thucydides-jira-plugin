# Thucydides plugin for JIRA

This plugin updates JIRA issues referenced in Thucydides stories with links to the corresponding story reports.

When you are using Thucydides with JUnit, JUnit tests can implement acceptance criteria for particular stories. If you
are working with JIRA, these stories can be represented as JIRA issues. You can refer to a JIRA issue by placing a
reference to the corresponding JIRA issue number in the @Title annotation, as shown here:

    @RunWith(ThucydidesRunner.class)
    @Story(Application.Search.SearchByKeyword.class)
    public class SearchByKeywordStoryTest {

        @Managed
        public WebDriver webdriver;

        @ManagedPages(defaultUrl = "http://www.wikipedia.com")
        public Pages pages;

        @Steps
        public EndUserSteps endUser;

        @Title("Searching by 'cat' should display the correspond cats article - (#THUCINT-1)")
        @Test
        public void searching_by_unambiguious_keyword_should_display_the_corresponding_article() {
            endUser.is_on_the_wikipedia_home_page();
            endUser.searches_for("cats");
            endUser.should_see_article_with_title("Cat - Wikipedia, the free encyclopedia");
        }
    }

You then specify the *jira.url* as a system property when you run the tests:

    $ mvn test -Djira.url=http://issues.acme.com

This will make Thucydides include a link to the corresponding JIRA issue in the Thucydides reports. This feature is
supported in the core Thucydides library.

For tighter, round-trip integration you can also use thucydides-jira-plugin. This will not only include links to JIRA
in the Thucydides reports, but it will also update the corresponding JIRA issues with links to the corresponding
Story page in the Thucydides reports. To set this up, add the `thucydides-jira-plugin` dependency to your project
dependencies:

    <dependencies>
        ...
        <dependency>
            <groupId>net.thucydides.plugins.jira</groupId>
            <artifactId>thucydides-jira-plugin</artifactId>
            <version>0.5.0</version>
        </dependency>
    </dependencies>

Then run the tests with the *jira.url*, *jira.username* and *jira.password* system parameters. These last two
parameters need to be a user who can connect to JIRA and add and update comments.

You also need to provide the base URL for the published Thucydides report in the system property
*thucydides.reports.url*. You would typically point this to the latest Thucydides reports on your build server.
(If you are using Jenkins, you can use the HTML Publisher plugin for this):

    $ mvn test -Djira.url=http://issues.acme.com -Djira.username=scott -Djira.password=tiger \
               -Dthucydides.reports.url= http:://jenkins.acme.com/myproject/job/webtests/Thucydides_Report

If you do not want Thucydides to update the JIRA issues for a particular run (e.g. for testing or debugging purposes),
you can also set *thucydides.skip.jira.updates* to true, e.g.

    $mvn verify -Dthucydides.skip.jira.updates=true

This will simply write the relevant issue numbers to the log rather than trying to connect to JIRA.

You can also configure the plugin to update the status of JIRA issues. This is deactivated by default: to use this
option, you need to set the 'thucydides.jira.workflow.active' option to 'true', e.g.

    $mvn verify -Dthucydides.jira.workflow.active=true

The default configuration will work with the default JIRA workflow: open or in progress issues associated with successful tests will be
resolved, and closed or resolved issues associated with failing tests will be reopened. If you are using a customized
workflow, or want to modify the way the transitions work, you can write your own workflow configuration. Workflow
configuration uses a simple Groovy DSL. The following is an example of the configuration file used for the default
workflow:

    when 'Open', {
        'success' should: 'Resolve Issue'
    }

    when 'Reopened', {
        'success' should: 'Resolve Issue'
    }

    when 'Resolved', {
        'failure' should: 'Reopen Issue'
    }

    when 'In Progress', {
        'success' should: ['Stop Progress','Resolve Issue']
    }

    when 'Closed', {
        'failure' should: 'Reopen Issue'
    }

You can write your own configuration file and place it on the classpath of your test project (e.g. in the `resources` directory). Then
you can override the default configuration by using the 'thucydides.jira.workflow' property, e.g.

    $mvn verify -Dthucydides.jira.workflow=my-workflow.groovy

Alternatively, you can simply create a file called 'jira-workflow.groovy' and place it somewhere on your classpath (e.g. in the 'src/test/resources' directory).
Thucydides will then use this workflow. In both these cases, you don't need to explicitly set the 'thucydides.jira.workflow.active'
property.



