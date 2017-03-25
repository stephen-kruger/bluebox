# Bluebox
Many teams develop software that rely heavily on sending emails (for sign-up, social software notifications etc). Properly testing this software requires multiple "fake" email addesses to allow testing of the capabilities of the software from various points of view and roles Traditionally we either used Google Mail (which allowed random prefixes to be added to email addresses), or other services such as Mailinator However sending such emails from unreleased products, potentially containing trade secrets, non-public product information and other private information to the public internet is not feasible, and an internal service was needed.

Bluebox is a fully functional promiscuous, non-forwarding SMTP server and mailbox web interface to allow receiving of arbitrary emails for use in testing and development environments. Inspired by the very useful Mailinator service. Provides API for scripted use by test automation.

# Installing
## Installing in Websphere Liberty
Download and install the latest Liberty profile from https://developer.ibm.com/assets/wasdev/

Create a Liberty profile :
```
<wlp_root>/wlp/bin/server create bluebox
```

Then navigate to the newly created profile (generally <wlp_root>/wlp/usr/servers/bluebox) and edit the server.xml.
An example is given here for reference :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<server description="bluebox server">

    <!-- Enable features -->
    <featureManager>
        <feature>jaxrs-2.0</feature>
        <feature>jaxws-2.2</feature>
        <feature>jsp-2.3</feature>
        <feature>servlet-3.1</feature>
        <feature>appSecurity-2.0</feature>
    </featureManager>

        <basicRegistry id="defaultRegistry">
                <group id="bluebox" name="bluebox">
                        <member id="bluebox" name="bluebox"/>
                </group>
                <user id="bluebox" name="bluebox" password="password"/>
        </basicRegistry>


    <!-- To access this server from a remote client add a host attribute to the following element, e.g. host="*" -->
    <httpEndpoint id="defaultHttpEndpoint"
                host="*"
                  httpPort="8080"
                  httpsPort="8443" />

    <!-- Automatically expand WAR files and EAR files -->
    <applicationManager autoExpand="true"/>
    <webApplication id="bluebox" location="bluebox.war" context-root="/"
        name="bluebox">
        <classloader apiTypeVisibility="spec, ibm-api, api, third-party" />
        <application-bnd>
                <security-role name="bluebox" id="bluebox">
                        <user name="bluebox" id="bluebox" access-id="bluebox"></user>
                </security-role>
        </application-bnd>
    </webApplication>
</server>
```

Download the war file from here :https://github.com/stephen-kruger/bluebox/releases and copy it to the <wlp_root>/wlp/user/servers/bluebox/apps directory

You can then start the server by running the command <wlp_root>/wlp/bin/server start bluebox

## Database options
On startup, if Bluebox detects a running instance of MongoDB, it will automatically set up and use that. Otherwise it will use an embedded H2 database which is a little less performant than MongoDB.

## Check the SMTP server
For security reasons the SMTP server will start on port 2500 instead of the expected port 25. So to recieve emails you need to either map port 25 to 2500, or change the config (see Additional Customisation section below)

### Option A: Map the port
On Linux run this command
```iptables -t nat -A PREROUTING -i eth0 -p tcp -m tcp --dport 25 -j REDIRECT --to-ports 2500```
### Option B: Change the config
On Windows systems, or if you don't mind starting the application server as root, you can change the default settings overriding them in bluebox.properties This file will be checked for in the Websphere Liberty server directory (e.g. [wlp dir]/usr/server/defaultServer/bluebox.properties).
Some commonly used properties are :
```properties
# SMTP server port
bluebox_port=2500
# maximum mail size in bytes
bluebox_mail_limit=100000000
# how many hours to keep trashed messages
bluebox_trash_age=12
# how many hours to keep messages (1344 = 8 weeks, 2688 = 16 weeks)
bluebox_message_age=5376
# trim messages (oldest first) to never exceed this limit
bluebox_message_max=160000
# frequency in minutes to run the expiry daemon
bluebox_daemon_delay=60
```

### Verify the smtp server is working
You can test it using simple telnet to see if the SMTP server responds on the default SMTP port:
```
telnet myhost.name 25
220 myhost.name ESMTP BlueBox V4.5.2
```

# FAQ
## How do I use it?
The easiest way : simply send an email to any address you like using the @bluebox.xxx.com domain For example send a test mail using your Notes client to test@bluebox.xxx.com or fake.mail@bluebox.xxx.com Then enter the email address into the form here at BlueBox? to show your inbox Another way : set your SMTP server to bluebox.xxx.com, you can send emails to any address and any domain (although they will all be delivered and available only on bluebox.xxx.com)

## How long will my email be available?
Messages will be automatically deleted after 48 hours Mails in the Trash folder (i.e. explicitly deleted by the user)will be removed after 12 hours If you require it for longer, simply use the Download button to save a copy to disk, or attach to a defect report You can then use the Upload functionality to re-inject it into the system as many times as you like

## SMTP Open Relay Test scans are complaining Bluebox is an open relay
These scans generally test if an SMTP server accepts email from arbitrary domains. By design, Bluebox IS very forgiving, and will accept any messages unless otherwise configured via blacklist or whitelist However no mails, regardless of recipient, are EVER forwarded on to other systems. So any scan which implies Bluebox is an open relay is not correctly implemented, as they would need to validate that a test email is actually delivered to a different MX domain - but generally these tests are lazily written, and only go as far as testing that the SMTP server performs the "accept" handshake. It is possible to defend against some of these poorly written relay scans by intelligently configuring the bluebox_to_blacklist or bluebox_from_blacklist in bluebox.properties: Run the offending scan program, and check the app server logs to detect what email is being used to run the test (e.g. test@microsoft.com), and then add that domain to the bluebox_to_blacklist, which will cause rejection of any messages from that domain, hopefully fooling the simple-minded Open Relay scan.

## Can I send mails to other domains?
BlueBox? itself does not do any mail forwarding, only incoming mail is accepted It's not very scrupulous about destination addresses, so if you have set bluebox as your smtp server, you can send to any hostnames that you like. Such as stephen@yahoo.com, steve@xxx.com, or any other domain you might want

# Terms and conditions of usage
Please do not use BlueBox for any business critical applications Please respect others may be using the system - don't delete mails which are not yours This software is provided as is. Availability and support is limited to the goodwill of the author, or patches you may wish to submit

# License
Copyright 2016 Stephen Kruger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.