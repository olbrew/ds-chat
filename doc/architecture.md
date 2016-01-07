Architecture
============

Avro
----
As the project assignment requested we used the Apache AVRO framework for Remote
Method Invocations.
We have two models, one for the server as defined in `src/chat.avpr` and one,
similar to the server for the client Server as defined in
`src/chatClientServer.avpr`.
The server model is self-explanatory. The client uses it to execute methods on
the server via *RMIs* such as a `register` method to register a client to the
server.
We emulate a server push via a clients own local server on which the main server
can then execute methods. This is needed to get non-client initiated server
messages such as a public room message via `incomingMessage`.
This local server is also used to manage connections and send messages between
clients for a private chat. These clients don't need the server after the
initial connection has been setup, although the server can still send
other people's private chat requests to people who are already in a private
chat.

Model View
----------
We use the suggested `Cliche` library for setting up a CLI.
All `Cliche` functions are defined in the `clientUI.java` class. This serves as
the *view* from where functions in the *model* are called. The model never calls
the view.
This separation of logic makes it easier to maintain the codebase and possibly
add another view with a different library.

Synchronicity
-------------
We assume that not so many clients will connect to the server at one time that
it will struggle to respond to user requests.
So we don't use any callbacks at this time.
In the case where we would need asynchronous functionality, for example where
another user has to accept a private chat request, we can simulate this by
having a separate Cliche function `accept`. To make sure that this function is
not abused by users to setup a private chat with someone without interaction
from their chatpartner we keep a `pendingRequests` table.
Now connections can only be setup by the server when there was an explicit
request from the requesting user.

Threads
-------
Because we run almost everything in threads we must take care
to keep our code thread safe. For example we use a `HashTable` instead of
`HashMap` because it's synchronized.
We periodically (every 5s) check if all connected clients are still alive on
the server-side via the `user.isAlive()` check. This loop runs in a separate
thread so the server won't hang and would be able to accept new clients and
handle their requests. Same applies to the individual clients that need to
check whether the server is still alive or even their chat partner in case
they are in a private chat room. Threads also allow us to send messages,
both to the server and our chat partner, while we're video streaming.

Video
-----
We've used Xuggler library to decode separate frames of the video. These
frames / images are then transferred as bytes over already existing
Avro proxy that was setup between the clients. The receiver transforms
these bytes back to an image and displays it on a Swing window. Since
the idea is to mimic a real video chat, we send the same video in both
directions at the same time. Once either party decides to stop the video,
both clients will halt sending further frames.

Integration with RSVP Click Project
-----------------------------------
<!--- TODO -->

General
-------
We use the username as a unique identifier. The server only allows you to
register when your name has not been used yet by another active user. This model
is a bit simple, but serves our purpose at the moment.
We also offer extensive error tolerance for when joining and leaving chatrooms
and automatically exiting a user from the server or chatroom if the other has
left in the private room case or when he simply went offline.
When the server goes down unexpectedly, connected clients wait for max. 75s to
see if the server comes back online and then automatically reconnect.
