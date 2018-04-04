# Start
sudo docker run -p 8001:8001 -p 8002:8002 -p 8003:8003 --name wasp -it wasp

# Get certificate
sudo docker cp wasp:/home/user/srv/warcprox/warcprox-ca.pem .

# Restart (e.g., after CTRL-C)
sudo docker start wasp

