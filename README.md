![Saturn is great](https://cyberphone.github.io/doc/saturn/github-saturnlogo.svg)

## A (potentially) Universal Payment Authorization System

https://cyberphone.github.io/doc/saturn

## Testing Saturn locally using Tomcat 8

In *Tomcat Install Directory* <code>conf/server.xml</code> define:
```xml

    <Connector port="8442" address="0.0.0.0" protocol="org.apache.coyote.http11.Http11Protocol"
         maxThreads="150" SSLEnabled="true" scheme="https" secure="true"
         clientAuth="false" sslProtocol="TLS" 
         keystoreFile="C:\github.repositories\saturn\tls-certificates\server\localhost.p12"
         keystorePass="foo123"
         keystoreType="PKCS12"
         URIEncoding="UTF-8"/>    
```
Perform the following ANT commands:

```
ant -f C:\github.repositories\saturn\build.xml tomcat
ant -f C:\github.repositories\saturn\build.xml tomcat
ant -f C:\github.repositories\saturn\build.xml tomcat
```
