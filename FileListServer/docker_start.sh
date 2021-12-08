docker build . -t file_server
#docker run -d --rm -w /app/tc -p 5000:5000/udp -p 5001:5001/udp --name file_server --cap-add=NET_ADMIN file_server sleep 1000
docker run -d --rm -w /app/tc -p 5000:5000/udp -p 5001:5001/udp --name file_server --cap-add=NET_ADMIN file_server /bin/bash ./tc_policy.sh
docker exec -d -w /app -it file_server java -classpath ./FileListServer-1.0.0.jar:./lib/* server.FileListServer 5000
docker exec -d -w /app -it file_server java -classpath ./FileListServer-1.0.0.jar:./lib/* server.FileListServer 5001
docker logs --follow file_server
