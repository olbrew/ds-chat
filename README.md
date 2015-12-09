Distributed chat application with Apache AVRO
=============================================

INSTALL
-------

1. Recompile the Avro schemes into Java code using the following commands

        cd lib/
        java -jar avro-tools-1.7.7.jar compile -string protocol ../src/chat.avpr ../src/
        java -jar avro−tools−1.7.7.jar compile -string protocol ../src/chatClientServer.avpr ../src/

2. Import the project in Eclipse as an existing Java project.

3. First start the server `avro.chat.server/Server`, then the clients `avro.chat.client/Client`.

AUTHORS
-------
Eduard Besjentsev & Olivier Brewaeys
