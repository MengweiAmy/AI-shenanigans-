javac sampleclients/*.java
javac searchclient/*.java
java -jar server.jar -l levels/Firefly.lvl -c "java searchclient.Run" -g