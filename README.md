![Saturn is great](https://cyberphone.github.io/doc/saturn/github-saturnlogo.svg)

## A (potentially) Universal Payment Authorization System

https://cyberphone.github.io/doc/saturn

## Testing Saturn locally using Tomcat 8.5

Prerequisites: JDK 8 and ANT

Download/unzip the Saturn ZIP or clone the GIT repository to any free directory.

Define an environment variable <code>CATALINA_HOME</code> = <code>TOMCAT-INSTALL-DIRECTORY</code>

In <code>TOMCAT-INSTALL-DIRECTORY/conf/server.xml</code> define:
```xml

    <Connector port="8442" protocol="org.apache.coyote.http11.Http11NioProtocol"
               maxThreads="150" SSLEnabled="true">
        <SSLHostConfig>
            <Certificate certificateKeystoreFile="SATURN-INSTALL-DIRECTORY\tls-certificates\server\localhost.p12"
                         certificateKeystoreType="pkcs12"
                         certificateKeystorePassword="foo123"/>
        </SSLHostConfig>
    </Connector>
```
Perform the following ANT commands:

```
$ ant -f SATURN-INSTALL-DIRECTORY/merchant/build.xml tomcat
$ ant -f SATURN-INSTALL-DIRECTORY/acquirer/build.xml tomcat
$ ant -f SATURN-INSTALL-DIRECTORY/bank/build.xml both
```
Update the <code>cacerts</code> file used by the JDK installation:

```
$ keytool -importcert -keystore JDK-INSTALL-DIRECTORY/jre/lib/security/cacerts -storepass changeit -file SATURN-INSTALL-DIRECTORY/tls-certificates/root/tlsroot.cer -alias webpkitestroot
```
Now Tomcat can be started.  There should be no errors in the log.

A browser can be used to invoke the "mechant" application at: http://localhost:8080/webpay-merchant

Using HTTPS requires installation of <code>SATURN-INSTALL-DIRECTORY/tls-certificates/root/tlsroot.cer</code> in the browser's trust store.  Then you would browse to the address: https://localhost:8442/webpay-merchant
