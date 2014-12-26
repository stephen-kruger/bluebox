#!/bin/bash
# this script updates svn and builds the new rpm
export SCRIPT_DIR=`dirname "$BASH_SOURCE"`
export BLUEBOX_SRC=$SCRIPT_DIR/..
cd $BLUEBOX_SRC
export BLUEBOX_SRC=`pwd`
export BLUEBOX_GIT=$SCRIPT_DIR/../bluebox.git
cd $BLUEBOX_GIT
export BLUEBOX_GIT=`pwd`

echo "Copying new repo info from $BLUEBOX_SRC to Github root $BLUEBOX_GIT/yum"
cp -R $BLUEBOX_SRC/target/rpm/bluebox/RPMS/noarch $BLUEBOX_SRC/../bluebox.git/yum

echo "Creating local Yum repository in $BLUEBOX_GIT/noarch"
createrepo $BLUEBOX_GIT/noarch

#echo "Checking rpm info"
#rpm -qip `find $BLUEBOX_GIT/yum -name bluebox*.rpm`

git add yum
git commit -m "Updating yum repo with new build"
git push git@github.com:stephen-kruger/bluebox.git gh-pages

sudo yum clean expire-cache
