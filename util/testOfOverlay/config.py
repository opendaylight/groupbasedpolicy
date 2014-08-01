

TENANT="f5c7d344-d1c7-4208-8531-2c2693657e12"
L3CTX="f2311f52-890f-4095-8b85-485ec8b92b3c"
L2BD="70aeb9ea-4ca1-4fb9-9780-22b04b84a0d6"

L2FD1="252fbac6-bb6e-4d16-808d-6f56d20e5cca"
EG1="1eaf9a67-a171-42a8-9282-71cf702f61dd"
L2FD2="cb5249bb-e896-45be-899d-4cdd9354b58e"
EG2="e593f05d-96be-47ad-acd5-ba81465680d5"

CONTRACT="22282cca-9a13-4d0c-a67e-a933ebb0b0ae"

switches = [{'name': 's1',
             'tunnelIp': '10.160.9.20',
             'dpid': '1'},
            {'name': 's2',
             'tunnelIp': '10.160.9.21',
             'dpid': '2'}]

hosts = [{'name': 'h35_2',
          'mac': '00:00:00:00:35:02',
          'ip': '10.0.35.2/24',
          'switch': 's1',
          'tenant': TENANT,
          'endpointGroup': EG1},
         {'name': 'h35_3',
          'ip': '10.0.35.3/24',
          'mac': '00:00:00:00:35:03',
          'switch': 's1',
          'tenant': TENANT,
          'endpointGroup': EG1},
         {'name': 'h35_4',
          'ip': '10.0.35.4/24',
          'mac': '00:00:00:00:35:04',
          'switch': 's2',
          'tenant': TENANT,
          'endpointGroup': EG1},
         {'name': 'h35_5',
          'ip': '10.0.35.5/24',
          'mac': '00:00:00:00:35:05',
          'switch': 's2',
          'tenant': TENANT,
          'endpointGroup': EG1},
         {'name': 'h36_2',
          'ip': '10.0.36.2/24',
          'mac': '00:00:00:00:36:02',
          'switch': 's1',
          'tenant': TENANT,
          'endpointGroup': EG2},
         {'name': 'h36_3',
          'ip': '10.0.36.3/24',
          'mac': '00:00:00:00:36:03',
          'switch': 's1',
          'tenant': TENANT,
          'endpointGroup': EG2},
         {'name': 'h36_4',
          'ip': '10.0.36.4/24',
          'mac': '00:00:00:00:36:04',
          'switch': 's2',
          'tenant': TENANT,
          'endpointGroup': EG2},
         {'name': 'h36_5',
          'ip': '10.0.36.5/24',
          'mac': '00:00:00:00:36:05',
          'switch': 's2',
          'tenant': TENANT,
          'endpointGroup': EG2}]

contracts = [{'consumer': EG1,
              'provider': EG2,
              'id': CONTRACT}]
