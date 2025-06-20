Compile and Run Both proxies using javac and java from system jdk.

Compile Both java files. (Both proxy)

# Ship Proxy (client)
cd proxy-system/ship-proxy/src
javac ShipProxy.java

# Offshore Proxy (server)
cd proxy-system/offshore-proxy/src
javac OffshoreProxy.java

Run Server Proxy

cd proxy-system/ship-proxy/src
java ShipProxy

#This opens port 9000 and waits for the ship to connect.

Run client proxy

cd proxy-system/offshore-proxy/src
java OffshoreProxy

It connects to offshore on port 9000 and starts listening on port 8080.

#Test with curl.

curl -x http://localhost:8080 http://example.com

This should give you an html template of the given url as response. 


