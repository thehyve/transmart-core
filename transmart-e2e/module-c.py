"""
Draft of e2e tests for Arvados integration.
"""
import requests as rq

host = 'http://localhost'
port = ':8080'
username = 'admin'
password = 'admin'
authUrl = host + port + '/oauth/token?grant_type=password&client_id=glowingbear-js&client_secret=&username={0}&password={1}&url={2}'.format(username, password, host)
baseUrl = host + port +'/v2/'
auth_response = rq.post(authUrl)
headers = {'Authorization': 'Bearer {}'.format(auth_response.json()['access_token']),
'Accept':'application/json'
}

data = {"uuid":"bla",
        "arvadosInstanceUrl":"a",
        "name":"name",
        "description":"sc",
        "arvadosVersion":"v1",
	    "defaultParams": {
		"a":1, "b":"b"
		},
	 }
new_source_system = {
	'name':'Arvbox at The Hyve',
    'systemType':'Arvados',
	'url':'http://arvbox-pro-dev.thehyve.net/',
    'systemVersion':'v1',
    'singleFileCollections':False,
}
new_file_link = {'name'        : 'new file Link',
                 'sourceSystem': None, # will be filled in after new_storage_system is created
                 'study'       : 'EHR',
                 'uuid'        : 'aaaaa-bbbbb-ccccccccccccccc',
}
response = rq.post(baseUrl + "storage", headers = headers, json = new_source_system)
print(response)
print(response.json())
assert response.ok
storage_id = response.json()['id']
new_file_link['sourceSystem'] = storage_id

response = rq.post(baseUrl + "files", headers = headers, json = new_file_link)
print(response)
print(response.json())
assert response.ok
link_id = response.json()['id']

response = rq.get(baseUrl + "storage", headers = headers)
print(response)
print(response.json())
assert response.ok

response = rq.post(baseUrl + "arvados/workflows", headers = headers, json= data)
print(response)
print(response.json())
assert response.ok
workflow_id = str(response.json()['id'])

response = rq.get(baseUrl + "arvados/workflows", headers = headers)
print(response)
print(response.json())
assert response.ok

response = rq.get(baseUrl + "arvados/workflows/" + workflow_id, headers = headers)
print(response)
print(response.json())
assert response.ok

response = rq.delete(baseUrl + "arvados/workflows/" + workflow_id, headers = headers)
print(response)
assert response.ok

response = rq.delete(baseUrl + "files/" + str(link_id), headers = headers)
print(response)
assert response.ok

response = rq.delete(baseUrl + "storage/" + str(storage_id), headers = headers)
print(response)
assert response.ok
