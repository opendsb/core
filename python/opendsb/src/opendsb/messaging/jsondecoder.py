# -*- coding: utf-8 -*-

import json

def deep_decoder(json_string):
    def _decode_strings(obj):
        if isinstance(obj, dict):
            for key, value in obj.items():
                if isinstance(value, str):
                    try:
                        obj[key] = json.loads(value)
                    except ValueError:
                        pass
                else:
                    _decode_strings(value)
        elif isinstance(obj, list):
            for item in obj:
                if isinstance(item, str):
                    try:
                        item = json.loads(item)
                    except ValueError:
                        pass
                else:
                    _decode_strings(item)

    try:
        json_obj = json.loads(json_string)
        _decode_strings(json_obj)
        return json_obj
    except Exception as e:
        raise e
    #except ValueError as e:
        #return None
        #raise e

