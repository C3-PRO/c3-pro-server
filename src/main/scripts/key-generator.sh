# Generates keys for dec/enc messages in SQS queue
# Requirements: openssl
# Copy the generated public-c3pro.der file into the corresponding S3 bucket
# Copy the generated private-c3pro.der file into the corresponding directory in the BCH installation
openssl genrsa -out keypair.pem 2048
openssl rsa -in keypair.pem -outform DER -pubout -out public-c3pro.der
openssl pkcs8 -topk8 -nocrypt -in keypair.pem -outform DER -out private-c3pro.der