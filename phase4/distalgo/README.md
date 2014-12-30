##Dependencies
<a href="https://www.python.org" target="_blank">Python 3.4.1</a>

<a href="https://github.com/DistAlgo/distalgo/" target="_blank">DistAlgo 1.0.0b10</a>

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
$ dar main.da ../config/basic.json
```

Creates the directory `logs/basic_X/` to contain each process' log file, where X
is the next highest number out of the diectories named `logs/basic_X/`.
Ex: `logs/basic_1/` is created initially then, `logs/basic_2/`, `logs/basic_3/`, etc...

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

##CONTRIBUTIONS
Shane Harvey and Soumadip Mukherjee design the pseudo code for this project.
After understanding the design, we decided to spilt the work mostly by language.

Shane Harvey implemented this non-fault tolerant chain replication implementaion
in DistAlgo. Soumadip Mukherjee reviewed, tested, and helped debug the DistAlgo code.

Soumadip Mukherjee implemented the non-fault tolerant chain replication implementaion
in Java. Shane Harvey reviewed, tested, and helped debug the Java code.

To this end, we believe we distributed the work about evenly and total time spend on
each implementation was similar.

##OTHER COMMENTS
