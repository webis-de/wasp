# WASP under Windows

__In progress - not completely working yet.__

Under Windows 10, the Docker container works fine, but the certificate for using `warcprox` does not get loaded correctly, at least not in Firefox.

Convert the `.pem` keyfile to a `.pfx` keyfile as follows:

    docker exec -it wasp /bin/bash
    cd /home/user/srv/warcprox
    openssl pkcs12 -export -inkey warcprox-ca.pem -in warcprox-ca.pem -out warcprox-ca.pfx
    
