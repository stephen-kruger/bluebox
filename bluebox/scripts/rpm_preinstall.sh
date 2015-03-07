#!/bin/sh -e
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
	echo "1/4: Shutting down tomcat"
	/sbin/service tomcat6 stop
	echo "2/4: Shutting down MongoDB"
	/sbin/service mongod stop
	echo "3/4: Cleaning previous installation in ${tomcat.webapp.dir}/bluebox"
	rm -rf ${tomcat.webapp.dir}/bluebox
	echo "4/4: Cleaning Lucence lock file"
	rm -rf /usr/share/tomcat6/temp/bluebox4.lucene/write.lock
fi
echo "Installing bluebox war file"