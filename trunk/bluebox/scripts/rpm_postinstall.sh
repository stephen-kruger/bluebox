#!/bin/sh -e
echo "Completed war file installation"
if [ "$1" = "1" ]; then
	echo "Please start the application server : service tomcat6 start"
elif [ "$1" = "2" ];
	then
		echo "Restarting tomcat to complete installation..."
		echo "1/2: Starting MongoDB"
		/sbin/service mongod start
		echo "2/2: Starting tomcat"
		/sbin/service tomcat6 start
fi