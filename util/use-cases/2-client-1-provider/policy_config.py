L3CTX='cbe0cc07-b8ff-451d-8171-9eef002a8e80'
L2BD='7b796915-adf4-4356-b5ca-de005ac410c1'
# Only one tenant supported at this time.
tenants = [
           {'name':'GBPPOC',
            'id':'f5c7d344-d1c7-4208-8531-2c2693657e12',
            'subject-feature-instances':
            {'classifier-instance':
                [
                {'name': 'http-dest',
                'classifier-definition-id': '4250ab32-e8b8-445a-aebb-e1bd2cdd291f',
                'parameter-value': [
                    {'name': 'type',
                     'string-value': 'TCP'},
                    {'name': 'destport',
                     'int-value': '80'}
                ]},
                {'name': 'http-src',
                'classifier-definition-id': '4250ab32-e8b8-445a-aebb-e1bd2cdd291f',
                'parameter-value': [
                    {'name': 'type',
                     'string-value': 'TCP'},
                    {'name': 'sourceport',
                     'int-value': '80'}
                ]},
                {'name': 'icmp',
                'classifier-definition-id': '79c6fdb2-1e1a-4832-af57-c65baf5c2335',
                'parameter-value': [
                    {'name': 'proto',
                     'int-value': '1'}
                                    ]
                 }
                 ]
             }
            }
           ]

contracts = [
             {'name':'pingall+web',
              'id':'22282cca-9a13-4d0c-a67e-a933ebb0b0ae',
              'subject': [
                {'name': 'allow-http-subject',
                 'rule': [
                    {'name': 'allow-http-rule',
                     'classifier-ref': [
                        {'name': 'http-dest',
                         'direction': 'in'},
                        {'name': 'http-src',
                         'direction': 'out'}
                          ]
                     }
                          ]
                 },
                {'name': 'allow-icmp-subject',
                 'rule': [
                    {'name': 'allow-icmp-rule',
                     'classifier-ref': [
                        {'name': 'icmp'}
                                                  ]}
                          ]
                 }],
              'clause': [
                {'name': 'allow-http-clause',
                 'subject-refs': [
                    'allow-http-subject',
                    'allow-icmp-subject'
                    ]
                 }
                        ]
              }]
endpointGroups = [
                   {'name':'client1',
                    'providesContracts' : [], #List of contract names provided
                    'consumesContracts' : ['pingall+web'],
                    'id' : '1eaf9a67-a171-42a8-9282-71cf702f61dd',
                    },
                   {'name':'client2',
                    'providesContracts' : [], #List of contract names provided
                    'consumesContracts' : ['pingall+web'],
                    'id' : '5e6c787c-156a-49ed-8546-547bdccf283c',
                    },
                  {'name':'webserver',
                    'providesContracts' : ['pingall+web'], #List of contract names provided
                    'consumesContracts' : [],
                    'id' : 'e593f05d-96be-47ad-acd5-ba81465680d5',
                   }
                  ]
