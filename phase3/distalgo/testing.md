#Test File Documentation
This is the documentation for each test file. It describes the objective
of each configuration test file.


###msg_loss.json
    Objective: Tests ability of clients and servers to simulate message loss
    1. Server1 starts as HEAD and TAIL with message loss at 25%
    2. Client starts with loss also at 25%

###ack_check.json
    Objective: Tests using ACKs to remove updates from Sent
    1. Chain of 3 servers is started with one client.
    2. Client issues deposits and withdrawal requests
    3. ACKs are send from the TAIL and each server removes updates.

###head_fail.json
    Objective: Tests ability to remove failed HEAD
    1. 2 Servers start with zero delay.
    2. The head fails after 1 second.
    3. Master detects the failure due to a 0 ping count.
    4. Master sends becomeHead to the second server.
    5. Master sends updateHead to the client
    6. Client continues to issue requests.

###tail_fail.json
    Objective: Tests ability to remove failed TAIL
    1. 2 Servers start with zero delay.
    2. The tail fails after 1 second.
    3. Master detects the failure due to a 0 ping count.
    4. Master sends becomeTail to the first server.
    5. Master sends updateTail to the client
    6. Client continues to issue requests.

###internal_fail.json
    Objective: Tests ability to remove failed INTERNAL server
    1. 3 Servers start with zero delay. (S-, S, S+)
    2. The internal server (S) fails after 1 second.
    3. Master detects the S failed due to a 0 ping count.
    4. Master initiates internal failure algorithm
    5. S+ sends last sequence to Master
    6. Master forwards last seq to S-
    7. S- forwards updates in SENT set with SEQ > last_seq
    8. Client continues to issue requests.

###chain_ext.json
    Objective: Tests ability of master to extend the chain
    1. Server1 starts as HEAD and TAIL
    2. Server2 starts after a 5 second delay. (sends join chain request)
    3. Master sends extendChain to Server1
    4. Server1 fowards updates and bank information to Server2
    5. Server2 sends newTail to Master
    6. Master sends updateTail to all the clients

###internal_fail2.json
    Objective: remove failed internal process S and remove its predecessor S- when S- fails during removal of S, immediately after learning which updates in Sent^S- should be forwarded to S+

    1. 3 Servers start with zero delay. (S-, S, S+)
    2. The internal server (S) fails after 1 second.
    3. Master detects the S failed due to a 0 ping count.
    4. Master initiates internal failure algorithm
    5. S+ sends last sequence to Master
    6. Master forwards last seq to S-
    7. S- fails after receiving 'successorFailure'
    8. Master detects the S- failed due to a 0 ping count.
    9. Master tells S+ to become the HEAD
   10. Client continues to issue requests.

###internal_fail3.json
    Objective: remove failed internal process S and remove its successor S+ when S+ fails during removal of S, immediately after receiving the updates in Sent^S-

    1. 3 Servers start with zero delay. (S-, S, S+)
    2. The internal server (S) fails after 1 second.
    3. Master detects the S failed due to a 0 ping count.
    4. Master initiates internal failure algorithm
    5. S+ sends last sequence to Master
    6. Master forwards last seq to S-
    7. S- forwards updates in SENT set with SEQ > last_seq to S+
    8. S+ fails after recieving Sent^S- (it's 5th message)
    9. Master detects the S+ failed due to a 0 ping count.
   10. Master tells S- to become the TAIL
   11. Client continues to issue requests.

###ext_tail_fail.json
    Objective: chain extension when current tail fails during chain extension
    Similiar to graceful_abort.json except we introduce that the tail will fail before sending "doneSending" to the new tail. Master detects the failure of
    both servers and continues to extend the chain with the 4th server.

###graceful_abort.json
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
   12. Client issues requests the whole time.

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
