# Example Message Flow for GP2GP Adaptor

## 1 - EHR Request

The adaptor receives the EHR Request message on the MHS inbound queue.

## 2 - GPC Access Structured

The adaptor requests the patient's structured records.

Create the certificate and key files using values provided by OpenTest before running `request.sh`.

`endpoint.crt` - VPN endpoint certificate
`endpoint.key` - VPN endpoint private key
`opentest.ca-bundle` - root and sub-CA certs copied into the same file

Generate a new Authorization token. The easiest way is to run a Postman request against
the public demonstrator and copy the header value it generates.

## 3 - GPC Find Documents

The adaptor finds all documents for the patient. This involves calling an additional endpoint to retrieve the full list of documents.

## 4 - GPC Access Document

The adaptor downloads all documents for the patient.

The same instructions as for "2 - GPC Access Structured" apply.
