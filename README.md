#Bluebox
Many teams develop software that rely heavily on sending emails (for sign-up, social software notifications etc). Properly testing this software requires multiple "fake" email addesses to allow testing of the capabilities of the software from various points of view and roles Traditionally we either used Google Mail (which allowed random prefixes to be added to email addresses), or other services such as Mailinator However sending such emails from unreleased products, potentially containing trade secrets, non-public product information and other private information to the public internet is not feasible, and an internal service was needed.

Bluebox is a fully functional promiscuous, non-forwarding SMTP server and mailbox web interface to allow receiving of arbitrary emails for use in testing and development environments. Inspired by the very useful Mailinator service. Provides API for scripted use by test automation.

#Installing
##Via RPM
You can set up RPM based installation by adding the following to bluebox.repo in your /etc/yum.repos.d directory :
```
[bluebox]
name=Bluebox Repository
baseurl=https://stephen-kruger.github.io/bluebox-repo/yum/noarch
enabled=1
gpgcheck=0
```
The simply run "sudo yum install bluebox"

##Manually using war file
Download the war file from here.

Drop the war file into your favourite application server.
This one requires no special setup, and uses an embedded Derby database.
If you want you can edit bluebox.properties and select the MongoDB driver instead - especially useful for larger mailboxes.
You will require Java 7.
access to the administration page requires user/password for role "tomcat", so edit your tomcat-users.conf file or whatever your app server uses

#FAQ
##How do I use it?
The easiest way : simply send an email to any address you like using the @bluebox.xxx.com domain For example send a test mail using your Notes client to test@bluebox.xxx.com or fake.mail@bluebox.xxx.com Then enter the email address into the form here at BlueBox? to show your inbox Another way : set your SMTP server to bluebox.xxx.com, you can send emails to any address and any domain (although they will all be delivered and available only on bluebox.xxx.com)

##How long will my email be available?
Messages will be automatically deleted after 48 hours Mails in the Trash folder (i.e. explicitly deleted by the user)will be removed after 12 hours If you require it for longer, simply use the Download button to save a copy to disk, or attach to a defect report You can then use the Upload functionality to re-inject it into the system as many times as you like

##SMTP Open Relay Test scans are complaining Bluebox is an open relay
These scans generally test if an SMTP server accepts email from arbitrary domains. By design, Bluebox IS very forgiving, and will accept any messages unless otherwise configured via blacklist or whitelist However no mails, regardless of recipient, are EVER forwarded on to other systems. So any scan which implies Bluebox is an open relay is not correctly implemented, as they would need to validate that a test email is actually delivered to a different MX domain - but generally these tests are lazily written, and only go as far as testing that the SMTP server performs the "accept" handshake. It is possible to defend against some of these poorly written relay scans by intelligently configuring the bluebox_to_blacklist or bluebox_from_blacklist in bluebox.properties: Run the offending scan program, and check the app server logs to detect what email is being used to run the test (e.g. test@microsoft.com), and then add that domain to the bluebox_to_blacklist, which will cause rejection of any messages from that domain, hopefully fooling the simple-minded Open Relay scan.

##Can I send mails to other domains?
BlueBox? itself does not do any mail forwarding, only incoming mail is accepted It's not very scrupulous about destination addresses, so if you have set bluebox as your smtp server, you can send to any hostnames that you like. Such as stephen@yahoo.com, steve@xxx.com, or any other domain you might want

#Terms and conditions of usage
Please do not use BlueBox for any business critical applications Please respect others may be using the system - don't delete mails which are not yours This software is provided as is. Availability and support is limited to the goodwill of the author, or patches you may wish to submit
