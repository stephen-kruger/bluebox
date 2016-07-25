FROM registry.eu-gb.bluemix.net/ibmliberty
COPY server.xml /opt/ibm/wlp/usr/servers/defaultServer/
ADD bluebox.war /opt/ibm/wlp/usr/servers/defaultServer/dropins/
ENV LICENSE accept
RUN /opt/ibm/wlp/bin/server start defaultServer 