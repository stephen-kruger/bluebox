mvn clean install -Dmaven.test.skip=true
~/Downloads/wlp/bin/server create bluebox
cp  target/bluebox.war ~/Downloads/wlp/usr/servers/bluebox/apps
cp server.xml  ~/Downloads/wlp/usr/servers/bluebox
 ~/Downloads/wlp/bin/installUtility install bluebox
