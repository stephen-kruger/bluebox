#!/bin/bash
export SCRIPT_DIR=`dirname "$BASH_SOURCE"`
export BLUEBOX_SRC=$SCRIPT_DIR/..

function command_exists () {
        if [ -e $1 ]; then
		return 1
	else
		return 0
	fi
}


function checkMaven() {
        if command_exists `which mvn`; then
        echo "Please install Maven:"
        echo "sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo"
        echo "sudo yum install apache-maven"
        exit
        else
                echo "Maven detected"
        fi
}

function checkSvn() {
        if command_exists `which svn`; then
        echo "Please install subversion"
        exit
        else
                echo "Subversion detected"
        fi
}
checkMaven
checkSvn

cd $BLUEBOX_SRC
export BLUEBOX_SRC=`pwd`

echo "Updating sources"
svn update
rm -rf $BLUEBOX_SRC/target/rpm/*

echo "Building sources"
mvn clean compile war:war rpm:rpm

echo "Creating local Yum repository in $BLUEBOX_SRC/target/rpm/bluebox/RPMS/noarch"
createrepo $BLUEBOX_SRC/target/rpm/bluebox/RPMS/noarch

echo "Checking rpm info"
rpm -qip `find $BLUEBOX_SRC -name bluebox*.rpm`
sudo yum clean all
