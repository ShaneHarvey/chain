#Test File Documentation
This is the documentation for each test file. It describes the objective
of each configuration test file.




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
