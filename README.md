Distributed chat application with Apache AVRO
=============================================
This is a simple Java chat application built with the [Apache AVRO framwork](https://avro.apache.org/) for *Remote Method Invocation*. It is built to work distributed on different devices.

INSTALL
-------

### Eclipse
1. Recompile the Avro schemes into Java code using the following commands

        cd lib/
        java -jar avro-tools-1.7.7.jar compile -string protocol ../src/chat.avpr ../src/
        java -jar avro−tools−1.7.7.jar compile -string protocol ../src/chatClientServer.avpr ../src/

2. Import the project in Eclipse as an existing Java project.

3. First start the Server, then the different clients.

4. Use our CLI for chatting

### CLI
0. Optional: Make sure files have been built by executing the former steps in Eclipse
1. Compile files
2. Go into the bin and run them

        cd bin/

Start the server on port 10010

        java -classpath ".:../lib/avro-1.7.7.jar:../lib/avro-ipc-1.7.7.jar:../lib/jackson-core-asl-1.9.13.jar:../lib/jackson-mapper-asl-1.9.13.jar:../lib/slf4j-api-1.7.7.jar:../lib/slf4j-simple-1.7.7.jar:../lib/asg.cliche-110413.jar" avro.chat.server.ChatServer 10010

Start the first client with following example values: Name 'Eduard', connect to the server running on IP address `127.0.0.1` with port `10010`, and give the port of the clientServer `11000`.

        java -classpath ".:../lib/avro-1.7.7.jar:../lib/avro-ipc-1.7.7.jar:../lib/jackson-core-asl-1.9.13.jar:../lib/jackson-mapper-asl-1.9.13.jar:../lib/slf4j-api-1.7.7.jar:../lib/slf4j-simple-1.7.7.jar:../lib/asg.cliche-110413.jar" avro.chat.client.ChatClient Eduard 127.0.0.1 10010 11000

You can start a second client to chat with. Eg.: Name 'Olivier', connect to the server running on IP address `127.0.0.1` with port `10010`, and give the port of the clientServer `11001`.

        java -classpath ".:../lib/avro-1.7.7.jar:../lib/avro-ipc-1.7.7.jar:../lib/jackson-core-asl-1.9.13.jar:../lib/jackson-mapper-asl-1.9.13.jar:../lib/slf4j-api-1.7.7.jar:../lib/slf4j-simple-1.7.7.jar:../lib/asg.cliche-110413.jar" avro.chat.client.ChatClient Olivier 127.0.0.1 10010 11001

AUTHORS
-------
Eduard Besjentsev & Olivier Brewaeys
