"""
Draft of e2e tests for Arvados integration.
"""
import requests as rq

baseUrl = 'http://localhost:8080/v2/arvados/'

headers = {'Authorization': 'Bearer 1784d43a-89e7-44bc-ad7b-3081e0f21259',
'Accept':'application/json'
}

data = {"uuid":"bla",
        "arvadosInstanceUrl":"a",
        "name":"name",
        "description":"sc",
        "arvadosVersion":"v1",
	    "defaultParams": {
		"a":1, "b":"b"
		}
	 }
response = rq.post(baseUrl + "workflows", headers = headers, json= data)
print(response)
print(response.json())
workflow_id = str(response.json()['id'])

response = rq.get(baseUrl + "workflows", headers = headers)
print(response)
print(response.json())

response = rq.get(baseUrl + "workflows/" + workflow_id, headers = headers)
print(response)
print(response.json())
