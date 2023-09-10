#!/usr/bin/env python3
from typing import Optional
from communication.android import AndroidLink
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

            self.android_link.send("Testing")

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
                self.android_dropped.set()
                self.logger.debug("Event set: Android connection dropped")

            self.logger.debug(msg_str)
                        
    def stop(self):
        """Stops all processes on the RPi and disconnects gracefully with Android and STM32"""
        self.logger.info("Program exited!")

if __name__ == "__main__":
    rpi = RaspberryPi()
    rpi.start()