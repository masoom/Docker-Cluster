/*set nodeslaveport*/

import hudson.model.*;
import jenkins.model.*;
Thread.start {
      sleep 10000
      println "--> setting agent port for jnlp"
      def env = System.getenv()
      int port = env['JENKINS_SLAVE_AGENT_PORT'].toInteger()
      Jenkins.instance.setSlaveAgentPort(port)
      println "--> setting agent port for jnlp... done"
}


/*set E-mail Notification*/
import jenkins.model.*
def instance = Jenkins.getInstance()
def desc = instance.getDescriptor("hudson.tasks.Mailer")
desc.setReplyToAddress("jennings.liu@sap.com")
desc.setSmtpHost("mailsin.sap.corp")
desc.setUseSsl(false)
desc.setSmtpPort("25")
desc.setCharset("UTF-8")
desc.save()



/*install plugin*/
import jenkins.model.*
import hudson.model.*;
def instance = Jenkins.getInstance()
def activatePlugin(plugin) {
        if (! plugin.isEnabled()) {
        plugin.enable()
        deployed = true
        }
        plugin.getDependencies().each {
            activatePlugin(pm.getPlugin(it.shortName))
        }
    }
Thread.start {
    sleep 10000
    pm = instance.pluginManager
    uc = instance.updateCenter
    pm.plugins.each { 
        plugin ->plugin.disable()
        }
    
    deployed = false

    
    ["git", "active-directory","role-strategy","github","gerrit-trigger"].each {
    if (! pm.getPlugin(it)) {
      println("Installing plugin $it")
      deployment = uc.getPlugin(it).deploy(true)
      deployment.get()
    
    }
    activatePlugin(pm.getPlugin(it))
    }
    
    if (deployed) {
    println("saving Jenkins configuration!")
    instance.save()
    }

}
/*set global security,set active directory*/

import jenkins.model.*
import hudson.security.*
import hudson.plugins.active_directory.*
   
def instance = Jenkins.getInstance()
Thread.start {
    sleep 10000
    String domain = 'my.domain.com'
    String site = 'site'
    String server = '192.168.1.1'
    String bindName = 'account@my.domain.com'
    String bindPassword = 'password'
    adrealm = new ActiveDirectorySecurityRealm(domain, site, bindName, bindPassword, server)
    instance.setSecurityRealm(adrealm)
}


/*Configuring Git Identity*/
def desc_git_scm = instance.getDescriptor("hudson.plugins.git.GitSCM")
Thread.start {
    sleep 10000
    println "--> Configuring Git Identity"
    desc_git_scm.setGlobalConfigName(gitGlobalConfigName)
    desc_git_scm.setGlobalConfigEmail(gitGlobalConfigEmail)
}



/*configure gerrit*/
import hudson.model.*;
import jenkins.model.*;
import com.sonyericsson.hudson.plugins.gerrit.trigger.PluginImpl;
import com.sonyericsson.hudson.plugins.gerrit.trigger.GerritServer;
import com.sonyericsson.hudson.plugins.gerrit.trigger.config.Config;


// Variables
def gerrit_host_name = env['GERRIT_HOST_NAME']
def gerrit_front_end_url = env['GERRIT_FRONT_END_URL']
def gerrit_ssh_port = env['GERRIT_SSH_PORT'] ?: "29418"
gerrit_ssh_port = gerrit_ssh_port.toInteger()
def gerrit_username = env['GERRIT_USERNAME'] ?: "jenkins"
def gerrit_profile = env['GERRIT_PROFILE'] ?: "ADOP Gerrit"
def gerrit_email = env['GERRIT_EMAIL'] ?: ""
def gerrit_ssh_key_file = env['GERRIT_SSH_KEY_FILE'] ?: "/var/jenkins_home/.ssh/id_rsa"
def gerrit_ssh_key_password = env['GERRIT_SSH_KEY_PASSWORD'] ?: null

// Constants
def instance = Jenkins.getInstance()

Thread.start {
    sleep 10000

    // Gerrit
    println "--> Configuring Gerrit"

    def gerrit_trigger_plugin = PluginImpl.getInstance()

    def gerrit_server = new GerritServer(gerrit_profile)

    def gerrit_servers = gerrit_trigger_plugin.getServerNames()
    def gerrit_server_exists = false
    gerrit_servers.each {
        server_name = (String) it
        if ( server_name == gerrit_server.getName() ) {
            gerrit_server_exists = true
            println("Found existing installation: " + server_name)
        }
    }

    if (!gerrit_server_exists) {
        def gerrit_server_config = new Config()

        gerrit_server_config.setGerritHostName(gerrit_host_name)
        gerrit_server_config.setGerritFrontEndURL(gerrit_front_end_url)
        gerrit_server_config.setGerritSshPort(gerrit_ssh_port)
        gerrit_server_config.setGerritUserName(gerrit_username)
        gerrit_server_config.setGerritEMail(gerrit_email)
        gerrit_server_config.setGerritAuthKeyFile(new File(gerrit_ssh_key_file))
        gerrit_server_config.setGerritAuthKeyFilePassword(gerrit_ssh_key_password)

        gerrit_server.setConfig(gerrit_server_config)
        gerrit_trigger_plugin.addServer(gerrit_server)
        gerrit_server.start()
        gerrit_server.startConnection()
    }

    // Save the state
    instance.save()
}
