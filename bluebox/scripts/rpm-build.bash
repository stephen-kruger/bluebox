#!/bin/bash
export SCRIPT_DIR=`dirname "$BASH_SOURCE"`
export BLUEBOX_SRC=$SCRIPT_DIR/..
export JAVA_HOME=/etc/alternatives/java_sdk_1.8.0
export PATH=$JAVA_HOME/bin:$PATH

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

function checkGit() {
        if command_exists `which git`; then
        echo "Please install git"
        exit
        else
                echo "Git detected"
        fi
}

function checkCreaterepo() {
        if command_exists `which createrepo`; then
        echo "Please install createrepo"
        exit
        else
                echo "Createrepo detected"
        fi
}

checkMaven
checkGit
checkCreaterepo

cd $BLUEBOX_SRC
export BLUEBOX_SRC=`pwd`

echo "Updating sources"
git pull
rm -rf $BLUEBOX_SRC/target/rpm/*

echo "Building sources"
mvn clean compile war:war rpm:rpm