#!/usr/bin/env python3
import requests
import json
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

    def start(self):
        """Starts the RPi orchestrator"""
        try:
            self.request_stitch()

        except KeyboardInterrupt:
            self.stop()

    def request_stitch(self):
        """Sends a stitch request to the image recognition API to stitch the different images together"""
        url = f"http://192.168.24.16:5000/status"
        response = requests.get(url)

        # If error, then log, and send error to Android
        if response.status_code != 200:
            # Notify android
            self.logger.error(
                "Something went wrong when requesting stitch from the API.")
            return
        
        results = json.loads(response.content)

        self.logger.info(results)
        
    def stop(self):
        """Stops all processes on the RPi and disconnects gracefully with Android and STM32"""
        self.logger.info("Program exited!")

if __name__ == "__main__":
    rpi = RaspberryPi()
    rpi.start()