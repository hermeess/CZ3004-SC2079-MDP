#!/usr/bin/env python3
from communication.stm32 import STMLink
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
        self.stm_link = STMLink()
        
        self.proc_recv_stm32 = None

    def start(self):
        """Starts the RPi orchestrator"""
        try:
            self.stm_link.connect()
            self.proc_recv_stm32 = Process(target=self.recv_stm)
            self.proc_recv_stm32.start()
            self.logger.info("Child Processes started")
            
            self.stm_link.send("12345")
            self.stm_link.send("54321")
            self.stm_link.send("abcd")
            self.stm_link.send("defg")

        except KeyboardInterrupt:
            self.stop()

    def recv_stm(self) -> None:
        """
        [Child Process] Receive acknowledgement messages from STM32, and release the movement lock
        """
        while True:
            message: str = self.stm_link.recv()
            self.logger.debug(message)
        
    def stop(self):
        """Stops all processes on the RPi and disconnects gracefully with Android and STM32"""
        self.stm_link.disconnect()
        self.logger.info("Program exited!")

if __name__ == "__main__":
    rpi = RaspberryPi()
    rpi.start()