#Test File Documentation
This is the documentation for each test file. It describes the objective
of each configuration test file.


###basic.json
    Objective: Tests basic bank functionality
    One server and one client are started.
    The client issues a deposit, balance, withdrawal, and balance requests
    on a single account.

###chain.json
    Objective: Test server chain functionality
    One bank with a chain of 3 servers and one client are started.
    The client issues a deposit, balance, withdrawal, and balance requests
    on a single account. (Same as the basic.json test)

###random_client.json
    Objective: Test that client can generate random requests
    One bank with a chain of 3 servers and one client are started.
    The client issues 5 random requests.

###clients.json
    Objective: Test that multiple clients can be instantiated
    One bank with a chain of 2 servers and 3 clients are started.
    2 clients issue random requests, the other issues the same request
    sequence the basic.json client

###banks.json
    Objective: Test that clients can interact with multiple bank chains
    3 banks with chains of varying and 5 clients are started.
    All 5 clients issue random requests.

###insufficient.json
    Objective: Test that insufficient funds works correctly
    One bank with a chain of 3 servers and one client are started.
    The client issues a deposit on $1337.00 into account '0001'. Next,
    the client issues a withdrawal of $1337.01 from account '0001' which
    should be illegal. The client should receive an insufficient funds reply.
    Lastly, the client issues a balance request on the same account.

###duplicate.json
    Objective: Test that duplicate request identification works correctly
    One bank with a chain of 3 servers and one client are started.
    First, the client issues a deposit on $111.00 into account '0001'.
    Second, the client issues a deposit of $222.00 into account '0001'.
    Third, the client issues the exact same request as the first which
    should be detected and the tail will resend the original reply stating
    that account '0001' has a balance of $111.00.
    Finally, the client issues a balance request on the same account to verify
    that the balance is actually $333.00.

###inconsistent.json
    Objective: Test that inconsistent request identification works correctly
    One bank with a chain of 3 servers and one client are started.
    First, the client issues a deposit on $111.00 into account '0001'.
    Second, the client issues a deposit of $222.00 into account '0001'.
    Third, the client issues the exact same request as the first **except that
    the amount is changed to $123.45** which should be detected and the tail
    will send an INCONSISTENT reply stating that account '0001' has a balance
    of $333.00.
    Fourth, the client issues a withdrawal request with the same reqID as the
    first. This should also trigger an INCONSISTENT reply.
    Finally, the client issues a balance request on the same account to verify
    that the balance is actually $333.00.


###test1.json and test2.json
    Used for developement testing

##Format Guide for JSON Files (if needed)
```
{
  "banks": {
// Contains any number of bank names.
    "Chase": {
      "chain": [
// This is the chain for each bank
// Each json object represents the configuration of that server
// The first configuration is the HEAD
// The last configuration is the TAIL
        {
          "ip": "localhost",
          "port": 50000,
          "start_delay": 0,
          "lifetime": 3,
          "receive": -1,
          "send": -1
        },
        ...
      ]
    },
    ...
  },
  "clients": [
// Each json object is the configuration for a single client
    {
      "reply_timeout": 3,
      "request_retries": 3,
      "resend_head": false,
      "requests": [
// If "requests" is a json array then it contains the ordered list of requests
// to send to the specified bank
        {
          "request": "deposit",
          "bank": "Chase",
          "account": "0001",
          "amount": "150.00",
          "seq_num": 0
        },
        ...
      ]
    },
    {
      "reply_timeout": 3,
      "request_retries": 3,
      "resend_head": false,
      "requests": {
// If "requests" is a json object then it contains the configuration to
// generate random requests.
        "seed": 2865189,
        "num_requests": 50,
        "prob_balance": 0.5,
        "prob_deposit": 0.3,
        "prob_withdrawal": 0.2,
        "prob_transfer": 0.0
      }
    },
    ...
  ]
}
```
