#!/bin/sh
if [ "$1" = "1" ]; then
	echo "Performing bluebox initial install..."
	echo "Set tomcat6 to start on boot"
	/sbin/chkconfig tomcat6 on
	echo "Mapping SMTP port 25 to 2500"
	/sbin/iptables -t nat -A PREROUTING -i eth0 -p tcp -m tcp --dport 25 -j REDIRECT --to-ports 2500
	echo "Persist across reboots"
	/sbin/chkconfig iptables on
	/sbin/service iptables save
elif [ "$1" = "2" ]; then
	echo "Performing bluebox upgrade..."
	echo "1/3: Shutting down tomcat"
	/sbin/service tomcat6 stop
	echo "2/3: Shutting down MongoDB"
	/sbin/service mongod stop
	echo "3/3: Cleaning previous installation in /usr/share/tomcat6/webapps/bluebox"
	rm -rf /usr/share/tomcat6/webapps/bluebox*
fi
echo "Installing bluebox war file"