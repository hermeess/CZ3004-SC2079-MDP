#!/usr/bin/env python3
import json
from typing import Optional
from communication.android import AndroidLink, AndroidMessage
from multiprocessing import Process
from logger import prepare_logger

class RaspberryPi:
    """
    Class that represents the Raspberry Pi.
    """
    def __init__(self):
        """
        Initializes the Raspberry Pi.
        """
        self.logger = prepare_logger()
        self.android_link = AndroidLink()

        self.proc_recv_android = None

    def start(self):
        """Starts the RPi orchestrator"""
        try:
            self.android_link.connect()
            self.proc_recv_android = Process(target=self.recv_android)

            # Start child processes
            self.proc_recv_android.start()
            
            while True:
                pass

        except KeyboardInterrupt:
            self.stop()
            
    def recv_android(self) -> None:
        """
        [Child Process] Processes the messages received from Android
        """
        while True:
            msg_str: Optional[str] = None
            try:
                msg_str = self.android_link.recv()
            except OSError:
                self.logger.debug("Event set: Android connection dropped")

            if msg_str is None:
                continue

            message: dict = json.loads(msg_str)

            ## Command: Set obstacles ##
            if message['cat'] == "obstacles":
                self.logger.debug(
                    f"Set obstacles PiAction added to queue: {message}")

            ## Command: Start Moving ##
            elif message['cat'] == "control":
                if message['value'] == "start":
                    self.logger.info(
                            "Start command received, starting robot on path!")
                        
    def stop(self):
        """Stops all processes on the RPi and disconnects gracefully with Android and STM32"""
        self.logger.info("Program exited!")

if __name__ == "__main__":
    rpi = RaspberryPi()
    rpi.start()