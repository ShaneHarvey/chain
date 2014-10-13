##DEPENDENCIES:
	Java 8

##INSTRUCTIONS:
####If make is available on your system simply cd into the `src/` directory
    and type `make`
    ```bash
    $ cd src
    $ make all
    ```
    To run a specific configuration file:
    This command runs the chain replication with configuration file ../config/basic.json
    ```bash
    $ make test-basic.json
    ```
    Similarily, this command runs with file ../config/duplicate.json
    ```bash
    $ make test-duplicate.json
    ```

####If make is not available:

	Step1 : Make sure java is installed on the computer
	Step2 : go into the src/ directory
	Step3 : type in the following command. It will compile the Client source code
			javac Client.java
    Step4 : type in the following command. It will compile the Server source code
    		javac Server.java
   	Step5: type in the following command. It will compile the ExecSimulation source code. Keep in mind that you must include the class path to the json jar.
   			javac -cp ./json-simple-1.1.1.jar:. ExecSimulation.java
   	Step 6: To run the simulation, type in the following command. Not that the argument is the path passed in is the path to the config file you want to test with.
   		java -cp ./json-simple-1.1.1.jar:. ExecSimulation ../config/test1.json


##MAIN FILES:
 	Path to Server file:
 		./src/Server.java
 	Path to Client file:
 		./src/Client.java
 	Path to ExecSimulation file:
 		./src/ExecSimulation.java


##Log Files:
    Each process creates their own log file in `/logs/`


##BUGS AND LIMITATIONS:
	Not currently. I hope... :X


##CONTRIBUTIONS:
	Shane Harvey and Soumadip working on the psudeo code and design of the distributed system. After designing.

	Shane Harvey primarily worked on implementing the non fault tolerant system in DistAlgo. Soumadip Mukherjee helped fix bugs, tested the code, and provided valuable input.

	Soumadip Mukherjee primarily worked on implementing the non fault tolerant system in Java. Shane Harvey helped fix bugs, tested the code, and provided valuable input.

	In essence, we distributed the work evenly amongst ourselves.
