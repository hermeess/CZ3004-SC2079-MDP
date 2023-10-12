#!/usr/bin/env python3
import io
import json
import time
import picamera
import requests
from consts import SYMBOL_MAP
from logger import prepare_logger
from settings import API_IP, API_PORT


class PiAction:
    """
    Class that represents an action that the RPi needs to take.    
    """

    def __init__(self, cat, value):
        """
        :param cat: The category of the action. Can be 'info', 'mode', 'path', 'snap', 'obstacle', 'location', 'failed', 'success'
        :param value: The value of the action. Can be a string, a list of coordinates, or a list of obstacles.
        """
        self._cat = cat
        self._value = value

    @property
    def cat(self):
        return self._cat

    @property
    def value(self):
        return self._value


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
            ### Start up initialization ###
            self.snap_and_rec("1_C")
            while True:
                pass
        except KeyboardInterrupt:
            self.stop()

    def stop(self):
        """Stops all processes on the RPi and disconnects gracefully with Android and STM32"""
        self.logger.info("Program exited!")

    def snap_and_rec(self, obstacle_id_with_signal: str) -> None:
        """
        RPi snaps an image and calls the API for image-rec.
        The response is then forwarded back to the android
        :param obstacle_id_with_signal: the current obstacle ID followed by underscore followed by signal
        """
        obstacle_id, signal = obstacle_id_with_signal.split("_")
        self.logger.info(f"Capturing image for obstacle id: {obstacle_id}")
        url = f"http://192.168.24.16:5007/image"
        filename = f"{int(time.time())}_{obstacle_id}_{signal}.jpg"

        # capture an image
        stream = io.BytesIO()
        with picamera.PiCamera() as camera:
            camera.start_preview()
            time.sleep(1)
            camera.capture(stream, format='jpeg')

        self.logger.debug("Requesting from image API")

        image_data = stream.getvalue()
        response = requests.post(
            url, files={"file": (filename, image_data)}) ##To check

        if response.status_code != 200:
            self.logger.error(
                "Something went wrong when requesting path from image-rec API. Please try again.")
            return

        results = json.loads(response.content)

        self.logger.info(f"results: {results}")
        self.logger.info(
            f"Image recognition results: {results} ({SYMBOL_MAP.get(results['image_id'])})")

if __name__ == "__main__":
    rpi = RaspberryPi()
    rpi.start()