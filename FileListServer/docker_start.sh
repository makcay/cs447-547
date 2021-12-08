docker build . -t file_server

docker run -d --rm -w /app/tc -p 5000:5000/udp -p 5001:5001/udp --name file_server --cap-add=NET_ADMIN file_server /bin/bash ./tc_policy.sh

# following two lines runs server code in two ports 5000 and 5001
# You can comment them and run server in the docker manually. In this way you, can see server logs
# You can run server from cli of the docker simply by giving java -classpath ./FileListServer-1.0.0.jar:./lib/* server.FileListServer 5000
docker exec -d -w /app -it file_server java -classpath ./FileListServer-1.0.0.jar:./lib/* server.FileListServer 5000
docker exec -d -w /app -it file_server java -classpath ./FileListServer-1.0.0.jar:./lib/* server.FileListServer 5001

# for tc logs
docker logs --follow file_server
