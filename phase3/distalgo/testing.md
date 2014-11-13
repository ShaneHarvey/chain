#Test File Documentation
This is the documentation for each test file. It describes the objective
of each configuration test file.


###basic_extend.json
    Objective: Tests ability of master to extend the chain
    1. Server1 starts as HEAD and TAIL
    2. Server2 starts after a 5 second delay. (sends join chain request)
    3. Master sends extendChain to Server1
    4. Server1 fowards updates and bank information to Server2
    5. Server2 sends newTail to Master
    6. Master sends updateTail to all the clients

###abort_extend.json
    Objective: Tests graceful abort of extension when the extending server fails
    1. Server1 starts as HEAD and TAIL
    2. Server2 starts after a 1 second delay. (sends join chain request)
    3. Master sends extendChain to Server1
    4. Server3 starts after a 2 second delay. (sends join chain request)
    5. Master Buffers the 2nd join chain request, Server3 waits.
    6. Server2 fails after recieving the first message from Server1
    7. Server1 does not know of the failure and sets successor to Server2
    8. Master detects that Server2 has failed during extension.
    9. Master sends becomeTail to Server1
   10. Master sends extendChain to Server1 due to queued request by Server3
   11. Server3 successfuly joins the chain.
   12. A client issues requests the whole time.

##Format Guide for JSON Files (if needed)
```json
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
      //This is the time to wait inbetween each request
      "req_delay": 2.0,
      // Simulated drop percentage of sent and received messages
      "msg_loss": 0.01,
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
