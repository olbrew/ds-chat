* We use a `HashTable` instead of a `HashMap` because it's synchronized so we can use it in a threaded evironment. See [here](http://stackoverflow.com/questions/40471/differences-between-hashmap-and-hashtable).
* One proxy per transceiver
* username as identifier
* server runs in thread => everything must be thread safe!
* check `user.isAlive()` in separate thread
