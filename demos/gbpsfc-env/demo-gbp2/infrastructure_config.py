# Config for switches, tunnelIP is the local IP address.
switches = [
            {'name': 'sw1',
             'type': 'gbp',
             'dpid': '1'},
            {'name': 'sw2',
             'type': 'gbp',
             'dpid': '2'},
            {'name': 'sw3',
             'type': 'sflow',
             'dpid': '3'},
            {'name': 'sw4',
             'type': 'none',
             'dpid': '4'},
            {'name': 'sw5',
             'type': 'none',
             'dpid': '5'},
            {'name': 'sw6',
             'type': 'none',
             'dpid': '6'},
            {'name': 'sw7',
             'type': 'none',
             'dpid': '7'},
            {'name': 'sw8',
             'type': 'none',
             'dpid': '8'}
	    ]

defaultContainerImage='alagalah/odlpoc_ovs230'
#defaultContainerImage='ubuntu:14.04'

#Note that tenant name and endpointGroup name come from policy_config.py

hosts = [{'name': 'h35-2',
          'mac': '00:00:00:00:35:02',
          'ip': '10.0.35.2/24',
          'switch': 'sw1'},
         {'name': 'h35-3',
          'ip': '10.0.35.3/24',
          'mac': '00:00:00:00:35:03',
          'switch': 'sw1'},
         {'name': 'h35-4',
          'ip': '10.0.35.4/24',
          'mac': '00:00:00:00:35:04',
          'switch': 'sw1'},
         {'name': 'h35-5',
          'ip': '10.0.35.5/24',
          'mac': '00:00:00:00:35:05',
          'switch': 'sw1'},
         {'name': 'h36-2',
          'ip': '10.0.36.2/24',
          'mac': '00:00:00:00:36:02',
          'switch': 'sw2'},
         {'name': 'h36-3',
          'ip': '10.0.36.3/24',
          'mac': '00:00:00:00:36:03',
          'switch': 'sw2'},
         {'name': 'h36-4',
          'ip': '10.0.36.4/24',
          'mac': '00:00:00:00:36:04',
          'switch': 'sw2'},
         {'name': 'h36-5',
          'ip': '10.0.36.5/24',
          'mac': '00:00:00:00:36:05',
          'switch': 'sw2'}
          ]

