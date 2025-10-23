import logging
import os
import time
import platform
from logging.handlers import RotatingFileHandler

def configure_logger(
    name: str,
    level: str,
    destination: str,
    max_bytes: int = 10_000_000,
    backup_count: int = 2,
    fmt: str = "%(asctime)s - %(name)s - %(levelname)s - %(threadName)s - %(filename)s - %(funcName)s - %(message)s",
    path: str = "logs",
) -> logging.Logger:
    # Remove old log files
    full_path = os.path.abspath(path)
    print(f"Looking up for logger files in {full_path}.", flush=True)
    log_files = [file for file in os.listdir(full_path) if ".log" in file]
    if log_files:
        for file in log_files:
            try:
                my_path = os.path.join(path, file)
                os.remove(os.path.join(path, file))
            except Exception as e:
                print(f"Could not remove file {file}.")
                continue

    logger = logging.getLogger(name)
    logger.setLevel(level)

    if destination.lower() == "file":
        handler = RotatingFileHandler(
            os.path.join(f"{path}", f"{name}.log"),
            maxBytes=max_bytes,
            backupCount=backup_count,
        )
    elif destination.lower() == "console":
        handler = logging.StreamHandler()
    else:
        msg = "Invalid logging destination. Please choose between 'file' or 'console'."
        raise ValueError(msg)

    handler.setFormatter(logging.Formatter(fmt))
    logger.handlers.clear()
    logger.addHandler(handler)
    time.sleep(1)  # Waiting logger configuration
    return logger
