#!/bin/bash

curl -s http://localhost:8080/api/v1/rollups/rollup -XPOST -H'Content-type:application/json' -d '
	[
	    {
	        "metric_name": "test_metric",
	        "schedule": "*/10 * * * * ?",
	        "start_relative": {
	            "value": 5,
	            "unit": "minutes"
	        },
	        "end_relative": {
	            "value": 6,
	            "unit": "minutes"
	        },
	        "targets": [
	            {"name": "rollup1"}
	        ]
	    }
	]
'
