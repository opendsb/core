
class WebSocketConnectionLost(ConnectionError):
  """
  Exceção levantada quando uma conexão WebSocket (cliente-servidor) é perdida
  de forma inesperada, geralmente devido a um timeout, falha de rede ou
  fechamento não limpo. Herda da classe builtin ConnectionError
  """

  def __init__(self, code: int, message: str, original_exception_class_name: str):
    """
    Inicializa a exceção de perda de conexão WebSocket.

    Args:
        code (int): Código de erro específico (ex: código de fechamento do WebSocket).
        message (str): Mensagem de erro detalhada.
        original_exception_class (str): Nome da classe da exceção de I/O original.
    """
    # Chama o construtor da classe base (ConnectionError)
    super().__init__(f"Conexão WebSocket perdida (Código: {code}): {message}")

    # Atributos específicos
    self.code = code
    self.message = message
    self.original_exception_class = original_exception_class_name

  def __str__(self):
    """Representação em string personalizada."""
    return (
      f"<{self.__class__.__name__}> "
      f"[Code: {self.code}] "
      f"{self.message} "
      f"(Causa original: {self.original_exception_class})"
    )

