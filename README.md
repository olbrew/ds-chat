Distributed chat application with Apache AVRO
=============================================
This is a simple Java chat application built with the [Apache AVRO framwork](https://avro.apache.org/) for *Remote Method Invocation*. It is built to work distributed on different devices.

INSTALL
-------

### Eclipse
0. Compile the Avro schemes into Java code using the following commands

        cd lib/
        java -jar avro-tools-1.7.7.jar compile -string protocol ../src/chat.avpr ../src/
        java -jar avro-tools-1.7.7.jar compile -string protocol ../src/chatClientServer.avpr ../src/

1. Import the project in Eclipse as an existing Java project.

2. First start the Server, then the different clients.

3. Use our CLI for chatting

### CLI
0. Compile the Avro schemes into Java code using the following commands

        cd lib/
        java -jar avro-tools-1.7.7.jar compile -string protocol ../src/chat.avpr ../src/
        java -jar avro-tools-1.7.7.jar compile -string protocol ../src/chatClientServer.avpr ../src/

1. Compile the java code

        cd ..
        ant build

2. Start your server on port 10010 for example

        cd bin/
        java -classpath ".:../lib/avro-1.7.7.jar:../lib/avro-ipc-1.7.7.jar:../lib/jackson-core-asl-1.9.13.jar:../lib/jackson-mapper-asl-1.9.13.jar:../lib/slf4j-api-1.7.7.jar:../lib/slf4j-simple-1.7.7.jar:../lib/asg.cliche-110413.jar" avro.chat.server.ChatServer 10010

3. Start as many clients as you want. Use `help` for possible arguments.

        java -classpath ".:../lib/avro-1.7.7.jar:../lib/avro-ipc-1.7.7.jar:../lib/jackson-core-asl-1.9.13.jar:../lib/jackson-mapper-asl-1.9.13.jar:../lib/slf4j-api-1.7.7.jar:../lib/slf4j-simple-1.7.7.jar:../lib/asg.cliche-110413.jar" avro.chat.client.ChatClient help

AUTHORS
-------
Eduard Besjentsev & Olivier Brewaeys
