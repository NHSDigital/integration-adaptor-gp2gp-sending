# Mock MHS Adaptor

The Mock MHS Adaptor currently has one endpoint, which mocks the MHS Outbound API.

This is a **POST** request to `http://localhost:8081/mock-mhs-endpoint/`

## Known Request
- The request can be matched to a known reply
- Must have the request header `Interaction-Id` with value `RCMR_IN030000UK06`
- Must have a JSON request body of the form: `{"payload": "STRINGIFIED XML HERE"}`
- Publishes a stub JSON message onto the MHS inbound queue
- Produces an HTTP 202 response
- Returns a stub ebXML message in the response body

## Unknown Request
- The request cannot be matched to a known reply
- Nothing is published to the inbound queue
- Produces an HTTP 500 response
- Returns an internal server error HTML message in the response body
