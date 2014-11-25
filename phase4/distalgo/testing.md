#Test File Documentation
This is the documentation for each test file. It describes the objective
of each configuration test file.

duplicate_transfer.json: 
  This test file sends a transfer request and then sends the same transfer 
  request so see if the the servers detect the duplicate request.

insufficient_funds_transfer.json :
  This test file sends a transfer request that has an amount specified 
  which is higher than the amount in the bank account.

transfer_no_failure.json:
  This sends a transfer request that has has sufficient funds from one bank
  to another bank.

pseudo_random.json:
  This sends a transfer request while two other client's send random deposits
  and withdrawal requests.

bank2_head_fails.json:
  The head of Bank2 fails after receiving the transfer request from the tail 
  of Bank1. The Tail of Bank1 resends the transfer when he is informed by 
  master that the head head of Bank2 failed. 

bank1_tail_bank2_head_fail.json:
  The tail of Bank1 fails after sending the transfer to the head of Bank2 and
  the head of Bank2 receives the transfer and then fails without sending the 
  request down the chain. The new tail of the Bank1 sends the new head of 
  Bank2 the transfer request.


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
        {
          "request": "transfer",
          "bank": "Chase",
          "account": "0001",
          "amount": "150.00",
          "dest_bank": "TD",
          "dest_account": "0001"
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
