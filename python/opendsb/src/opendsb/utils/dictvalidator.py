

#TODO: Adicionar validador de tipos dos atributos


class DictValidator:
    
    @staticmethod
    def validate(dictionary: dict, schema: list[str], dict_id: str):
        """Validate a dictionary against a schema."""      
        for attr in schema:
            if attr not in dictionary:
                raise Exception(f'Attribute "{attr}" not found in an instance of the class "{dict_id}"')
        
    

