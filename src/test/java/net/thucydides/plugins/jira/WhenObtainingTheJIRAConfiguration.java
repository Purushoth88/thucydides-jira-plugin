package net.thucydides.plugins.jira;

import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.MockEnvironmentVariables;
import net.thucydides.plugins.jira.service.JIRAConfiguration;
import net.thucydides.plugins.jira.service.SystemPropertiesJIRAConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class WhenObtainingTheJIRAConfiguration {

    JIRAConfiguration configuration;
    EnvironmentVariables environmentVariables = new MockEnvironmentVariables();

    @Before
    public void saveSystemProperties() {
        configuration = new SystemPropertiesJIRAConfiguration(environmentVariables);
    }

    @Test
    public void username_should_be_specified_in_the_jira_username_system_property() {
        environmentVariables.setProperty("jira.username", "joe");
        assertThat(configuration.getJiraUser(), is("joe"));
    }

    @Test
    public void password_should_be_specified_in_the_jira_password_system_property() {
        environmentVariables.setProperty("jira.password", "secret");
        assertThat(configuration.getJiraPassword(), is("secret"));
    }

    @Test
    public void base_url_should_be_specified_in_the_jira_url_system_property() {
        environmentVariables.setProperty("jira.url", "http://build.server/jira");
        assertThat(configuration.getJiraWebserviceUrl(), is("http://build.server/jira/rpc/soap/jirasoapservice-v2"));
    }

}
