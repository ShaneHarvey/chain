##DEPENDENCIES:
Tested with Java 7 & 8

##INSTRUCTIONS:
####If make is available on your system
Simply cd into the `src/` directory
and type `make`
```bash
$ cd src
$ make
```
To run a specific configuration file:
This command runs the chain replication with configuration file `../config/basic.json`
```bash
$ make test-basic.json
```
Similarily, this command runs with file `../config/duplicate.json`
```bash
$ make test-duplicate.json
```

####If make is not available:

Step1 : Make sure java is installed on the computer
Step2 : go into the `src/` directory
Step3 : type in the following command. It will compile the `Client` and `Server` source code
```bash
$ javac Client.java Server.java
```
Step4: type in the following command. It will compile the `ExecSimulation` source code. Keep in mind that you must include the class path to the json jar.
```bash
$ javac -cp ./json-simple-1.1.1.jar:. ExecSimulation.java
```
Step 6: To run the simulation, type in the following command. Not that the argument is the path passed in is the path to the config file you want to test with.
```bash
$ java -cp ./json-simple-1.1.1.jar:. ExecSimulation ../config/test1.json
```
####Cleaning Up Servers
In this implementation the bank servers run **forever.** To clean up these processes the user needs to kill them, ie:
```bash
$ make test-chain.json
java -cp ./json-simple-1.1.1.jar:. ExecSimulation ../config/chain.json
Chase
Done Processing Servers
java Server HEAD localhost:50000 localhost:0 localhost:50001 localhost:0 0 3 -1 -1 Chase
java Server MIDDLE localhost:50001 localhost:50000 localhost:50002 localhost:0 0 4 -1 -1 Chase
java Server TAIL localhost:50002 localhost:50001 localhost:0 localhost:0 0 5 -1 -1 Chase
Client 1 started
$ ps -ax | grep java
 2549 pts/0    Sl     0:00 java -agentpath:/usr/lib64/libabrt-java-connector.so=abrt=on Server HEAD localhost:50000 localhost:0 localhost:50001 localhost:0 0 3 -1 -1 Chase
 2568 pts/0    Sl     1:40 java -agentpath:/usr/lib64/libabrt-java-connector.so=abrt=on Server MIDDLE localhost:50001 localhost:50000 localhost:50002 localhost:0 0 4 -1 -1 Chase
 2587 pts/0    Sl     1:40 java -agentpath:/usr/lib64/libabrt-java-connector.so=abrt=on Server TAIL localhost:50002 localhost:50001 localhost:0 localhost:0 0 5 -1 -1 Chase
 3072 pts/0    S+     0:00 grep --color=auto java
$ kill 3072
$ ps -ax | grep java
 3275 pts/0    S+     0:00 grep --color=auto java
$
```


##MAIN FILES:
Path to Server file: `./src/Server.java`  
Path to Client file: `./src/Client.java`  
Path to ExecSimulation file: `./src/ExecSimulation.java`


##Log Files:
Each process creates their own log file in `/logs/`
Server logs are named `BANKNAME_Server_PORTNUMBER.log  
Client logs are named `Client_PORTNUMBER.log

In sequential runs, if the log file with that name already exists the process
will **append to the file.**


##BUGS AND LIMITATIONS:
Not currently. I hope... :X


##CONTRIBUTIONS:
Shane Harvey and Soumadip working on the psudeo code and design of the distributed system. After designing.

Shane Harvey primarily worked on implementing the non fault tolerant system in DistAlgo. Soumadip Mukherjee helped fix bugs, tested the code, and provided valuable input.

Soumadip Mukherjee primarily worked on implementing the non fault tolerant system in Java. Shane Harvey helped fix bugs, tested the code, and provided valuable input.

In essence, we distributed the work evenly amongst ourselves.
