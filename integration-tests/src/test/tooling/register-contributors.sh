#!/bin/bash

API_URL=http://localhost:9000/api

function registerBody() {
  contributorId=$1
  income=$2
  cat <<EOF
{
	"contributorId": "$contributorId",
	"registrationDate": "2018-08-19T14:20:38Z",
	"previousYearlyIncome": $2,
	"incomeType": "real"
}
EOF
}

regionId=001
count=${1:-1}
income=20000
for i in $(seq 1 $count)
do
    r=$((1 + RANDOM % 1000))
    id=$((r * 10000 + i))
    contributorId=$(printf "%03d%015d" $regionId $id)
    curl -X POST \
      $API_URL/contributors \
      -H 'Content-Type: application/json' \
      -H 'User-Agent: PostmanRuntime/7.15.2' \
      -d "$(registerBody $contributorId $income)"
    income=$((income + 1))
done
