import json


def deep_decoder(json_string: str | bytes | bytearray) -> dict | list | None:
    def _decode_strings(obj: dict | list):
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
                        item = json.loads(item)  # noqa: PLW2901
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
    # except ValueError as e:
    # return None
    # raise e
