Log files are created inside this directory.
One directory is created for each test file.

```
$ dar main.da test2.json
```

Creates the directory `.test2_X/` to contain each process' log file, where X
is the next highest number out of the diectories named `.test2_\d+`.
Ex: `.test2_1/` is created initially then, `.test2_2/`, `.test2_3/`, etc...

Server process logs are named `server_BANKNAME_X.log` where X is the servers
intitial position in the chain, X=1 being the head server and X=n being the
tail of a chain of n servers.

Client process logs are named `client_X.log` where X is an arbitrary id.
