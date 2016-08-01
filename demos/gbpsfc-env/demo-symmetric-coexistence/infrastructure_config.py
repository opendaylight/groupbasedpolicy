# Config for switches, tunnelIP is the local IP address.
switches = [
            {'name': 'sw1',
             'type': 'gbp',
             'dpid': '1'},
            {'name': 'sw2',
             'type': 'gbp',
             'dpid': '2'},
            {'name': 'sw3',
             'type': 'sf',
             'dpid': '3'},
            {'name': 'sw4',
             'type': 'gbp',
             'dpid': '4'},
            {'name': 'sw5',
             'type': 'sf',
             'dpid': '5'},
            {'name': 'sw6',
             'type': 'none',
             'dpid': '6'}
	    ]

defaultContainerImage='alagalah/odlpoc_ovs230'

#Note that tenant name and endpointGroup name come from policy_config.py

hosts = [{'name': 'h35-2',
          'mac': '00:00:00:00:35:02',
          'ip': '10.0.35.2/24',
          'switch': 'sw1'},
         {'name': 'h35-3',
          'ip': '10.0.35.3/24',
          'mac': '00:00:00:00:35:03',
          'switch': 'sw2'},
         {'name': 'h35-4',
          'ip': '10.0.35.4/24',
          'mac': '00:00:00:00:35:04',
          'switch': 'sw4'},
         {'name': 'h36-2',
          'ip': '10.0.36.2/24',
          'mac': '00:00:00:00:36:02',
          'switch': 'sw1'},
         {'name': 'h36-3',
          'ip': '10.0.36.3/24',
          'mac': '00:00:00:00:36:03',
          'switch': 'sw2'},
         {'name': 'h36-4',
          'ip': '10.0.36.4/24',
          'mac': '00:00:00:00:36:04',
          'switch': 'sw4'}
          ]
