FROM registry.eu-gb.bluemix.net/ibmliberty
COPY target/server.xml /opt/ibm/wlp/usr/servers/defaultServer/
ADD target/bluebox.war /opt/ibm/wlp/usr/servers/defaultServer/dropins/
ENV LICENSE accept