##Dependencies
Python 3.4.1 =======> https://www.python.org

DistAlgo 1.0.0b9 ====> https://github.com/DistAlgo/distalgo/

##INSTRUCTIONS
Just cd into the src directory and invoke the DistAlgo runtime on `main.da`
```bash
$ cd src
$ dar main.da
```

`main.da` optionally takes a config file as its only argument. If no file is
supplied the default test file is `../config/basic.json`
```bash
$ dar main.da ../config/basic.json
```


##Log Files
Each process (clients/servers) creates its own logging file.
Each run generates a unique directory inside `logs/` to place all process log files.

Example
```bash
$ dar main.da ../config/banks.json
```

Creates the directory `logs/banks_X/` to contain each process' log file, where X
is the next highest number out of the diectories named `logs/banks_X/`.
Ex: `logs/banks_1/` is created initially then, `logs/banks_2/`, `logs/banks_3/`, etc...

Server process logs are named `server_BANKNAME_X.log` where X is the servers
intitial position in the chain, X=1 being the head server and X=n being the
tail of a chain of n servers.

Client process logs are named `client_X.log` where X is an arbitrary id.


##MAIN FILES
```
src/main.da
src/client.da
src/server.da
```

##BUGS AND LIMITATIONS
None? :)


##CONTRIBUTIONS
Shane Harvey and Soumadip Mukherjee design the pseudo code for this project.
After understanding the design, we decided to spilt the work mostly by language.

Shane Harvey implemented this non-fault tolerant chain replication implementaion
in DistAlgo.
Soumadip Mukherjee reviewed, tested, and helped debug the DistAlgo code.

Soumadip Mukherjee implemented the non-fault tolerant chain replication implementaion
in Java.
Shane Harvey reviewed, tested, and helped debug the Java code.


##OTHER COMMENTS
None?
