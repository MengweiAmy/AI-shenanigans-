javac sampleclients/*.java
javac searchclient/*.java
java -jar server.jar -l levels/MAsimple2.lvl -c "java searchclient.SearchClient" -g