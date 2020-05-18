FROM registry.bisnode.net/dbase/server-jre8:1.8.40

EXPOSE 5701
EXPOSE 8080

COPY build/libs/hzcast-*.jar /data/hzcast.jar
COPY shutdown.sh /kubernetes/hooks/preStop.d/20shutdown.sh

WORKDIR "/data"
CMD ["java", "-server", "-jar", "/data/hzcast.jar"]
